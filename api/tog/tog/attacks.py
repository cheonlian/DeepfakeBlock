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
    
# victim: target_model, x_query: image, iter: ?????? ??????, eps: ????????? ??????, eps_iter: gradient ??????
def shadow_attack(victim, x_query, n_iter=10, eps=8/255, eps_iter=1/255):
    # (1?????? ????????? ?????????) ????????? ?????? ?????? ????????? ?????? ???????????? ???????????? ??????
    def _tv_diff(channel):
        x_wise = channel[:, :, 1:] - channel[:, :, :-1]
        y_wise = channel[:, 1:, :] - channel[:, :-1, :]
        return x_wise, y_wise

    # (1?????? ????????? ?????????) ????????? ?????? ?????? ????????? ????????? TV??? ???????????? ??????
    def smooth_tv(channel):
        x_wise, y_wise = _tv_diff(channel)
        return (x_wise * x_wise).sum() + (y_wise * y_wise).sum()

    # ??????????????? 3?????? ????????? ?????? ???????????? ?????? TV??? ???????????? ??????
    def get_tv(input):
        return smooth_tv(input[:, 0]) + smooth_tv(input[:, 1]) + smooth_tv(input[:, 2])
    '''
    detections_query: ?????? yolo ????????? ?????? ????????? ?????? ??????
    eta: ????????? ??????
    '''
    detections_query = victim.detect(x_query, conf_threshold=victim.confidence_thresh_default)
    eta = np.random.uniform(-eps, eps, (1,416,416,1))
    target_label = tf.constant([0])
    
    for _ in range(n_iter):
        # shadow ?????????
        # np.repeat(?????? ??????, axis: ????????????)
        ct = eta.repeat(3, axis=3)
        current = np.clip(x_query + ct, 0.0, 1.0)
        grad = victim.compute_object_untargeted_gradient(current, detections=detections_query)
        # (1,416,416,1)????????? ??????
        grad = np.split(grad, 3, axis=3)[0]
        signed_grad = np.sign(grad)
        signed_grad += get_tv(ct) * 0.01
        # 3?????? ????????? ?????? ord ????????? None??? ???????????????(2-norm)
        signed_grad += np.linalg.norm(ct, None) * 0.000001
        diff = -eps_iter * signed_grad
        eta = np.clip(eta + diff, -eps, eps)
    
    current = np.clip(x_query + eta, 0.0, 1.0)
    return current