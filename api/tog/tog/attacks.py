from tog.attack_utils.target_utils import generate_attack_targets
import numpy as np
import tensorflow as tf


def tog_vanishing(victim, x_query, n_iter=10, eps=8/255., eps_iter=2/255.):
    eta = np.random.uniform(-eps, eps, size=x_query.shape)
    x_adv = np.clip(x_query + eta, 0.0, 1.0)
    for _ in range(n_iter):
        grad = victim.compute_object_vanishing_gradient(x_adv)
        signed_grad = np.sign(grad)
        x_adv -= eps_iter * signed_grad
        eta = np.clip(x_adv - x_query, -eps, eps)
        x_adv = np.clip(x_query + eta, 0.0, 1.0)
    return x_adv


def tog_fabrication(victim, x_query, n_iter=10, eps=8/255., eps_iter=2/255.):
    eta = np.random.uniform(-eps, eps, size=x_query.shape)
    x_adv = np.clip(x_query + eta, 0.0, 1.0)
    for _ in range(n_iter):
        grad = victim.compute_object_fabrication_gradient(x_adv)
        signed_grad = np.sign(grad)
        x_adv -= eps_iter * signed_grad
        eta = np.clip(x_adv - x_query, -eps, eps)
        x_adv = np.clip(x_query + eta, 0.0, 1.0)
    return x_adv


def tog_mislabeling(victim, x_query, target, n_iter=10, eps=8/255., eps_iter=2/255.):
    detections_query = victim.detect(x_query, conf_threshold=victim.confidence_thresh_default)
    detections_target = generate_attack_targets(detections_query, confidence_threshold=victim.confidence_thresh_default,
                                                mode=target)
    eta = np.random.uniform(-eps, eps, size=x_query.shape)
    x_adv = np.clip(x_query + eta, 0.0, 1.0)
    for _ in range(n_iter):
        grad = victim.compute_object_mislabeling_gradient(x_adv, detections=detections_target)
        signed_grad = np.sign(grad)
        x_adv -= eps_iter * signed_grad
        eta = np.clip(x_adv - x_query, -eps, eps)
        x_adv = np.clip(x_query + eta, 0.0, 1.0)
    return x_adv


def tog_untargeted(victim, x_query, n_iter=10, eps=8/255., eps_iter=2/255.):
    detections_query = victim.detect(x_query, conf_threshold=victim.confidence_thresh_default)
    eta = np.random.uniform(-eps, eps, size=x_query.shape)
    x_adv = np.clip(x_query + eta, 0.0, 1.0)
    for _ in range(n_iter):
        grad = victim.compute_object_untargeted_gradient(x_adv, detections=detections_query)
        signed_grad = np.sign(grad)
        x_adv -= eps_iter * signed_grad
        eta = np.clip(x_adv - x_query, -eps, eps)
        x_adv = np.clip(x_query + eta, 0.0, 1.0)
    return x_adv
    
# victim: target_model, x_query: image, iter: 학습 횟수, eps: 노이즈 정도, eps_iter: gradient 조절
def shadow_attack(victim, x_query, n_iter=10, eps=8/255, eps_iter=1/255):
    # (1개의 채널에 대하여) 인접한 픽셀 간의 차이를 담은 리스트를 반환하는 함수
    def _tv_diff(channel):
        x_wise = channel[:, :, 1:] - channel[:, :, :-1]
        y_wise = channel[:, 1:, :] - channel[:, :-1, :]
        return x_wise, y_wise

    # (1개의 채널에 대하여) 인접한 픽셀 간의 차이를 이용해 TV를 계산하는 함수
    def smooth_tv(channel):
        x_wise, y_wise = _tv_diff(channel)
        return (x_wise * x_wise).sum() + (y_wise * y_wise).sum()

    # 실질적으로 3개의 채널을 모두 활용하여 전체 TV를 계산하는 함수
    def get_tv(input):
        return smooth_tv(input[:, 0]) + smooth_tv(input[:, 1]) + smooth_tv(input[:, 2])
    '''
    detections_query: 최초 yolo 모델의 최초 이미지 탐지 결과
    eta: 노이즈 채널
    '''
    detections_query = victim.detect(x_query, conf_threshold=victim.confidence_thresh_default)
    eta = np.random.uniform(-eps, eps, (1,416,416,1))
    target_label = tf.constant([0])
    
    for _ in range(n_iter):
        # shadow 노이즈
        # np.repeat(반복 횟수, axis: 시작차원)
        ct = eta.repeat(3, axis=3)
        current = np.clip(x_query + ct, 0.0, 1.0)
        grad = victim.compute_object_untargeted_gradient(current, detections=detections_query)
        # (1,416,416,1)번쨰만 저장
        grad = np.split(grad, 3, axis=3)[0]
        signed_grad = np.sign(grad)
        signed_grad += get_tv(ct) * 0.01
        # 3차원 이상의 값은 ord 값으로 None을 넘겨줘야함(2-norm)
        signed_grad += np.linalg.norm(ct, None) * 0.000001
        diff = -eps_iter * signed_grad
        eta = np.clip(eta + diff, -eps, eps)
    
    current = np.clip(x_query + eta, 0.0, 1.0)
    return current