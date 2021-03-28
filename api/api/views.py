from django.shortcuts import render, HttpResponse
from django.http import FileResponse, response
from .serializers import ImageSerializer

from django.views import View
from .models import Image

from django.utils.decorators import method_decorator
from django.views.decorators.csrf import csrf_exempt
from rest_framework.decorators import api_view
from rest_framework.response import Response
from rest_framework import status

from PIL import Image as pilImage


@api_view(["GET"])
def index(request, id):
    image = Image.objects.get(id=id)
    serializer = ImageSerializer(image)
    return Response(serializer.data)



#attack
from tog.dataset_utils.preprocessing import letterbox_image_padded
from keras import backend as K
from tog.models.yolov3 import YOLOv3_Darknet53_Face
from tog.tog.attacks import *
import tensorflow as tf

K.clear_session()
global graph
graph = tf.get_default_graph()


tf_config = tf.ConfigProto()
tf_config.gpu_options.allow_growth = True
session = tf.Session(config=tf_config)


weights = "tog/model_weights/yolo_face.h5"
detector = YOLOv3_Darknet53_Face(weights=weights)

#detect
import cv2
import numpy as np

YOLO_net = cv2.dnn.readNetFromDarknet("yolov3/cfg/yolov3_face.cfg","yolov3/yolov3_face.weights")
with open("yolov3/data/face.names", "r") as f:
    classes = [line.strip() for line in f.readlines()]
layer_names = YOLO_net.getLayerNames()
output_layers = [layer_names[i[0] - 1] for i in YOLO_net.getUnconnectedOutLayers()]



# 모든 사이즈 공격 가능
def attack(input, noise):
    w, h = input.size
    newsize = (w + 416, h + 416)
    new_image = pilImage.new("RGB", newsize, (0, 0, 0))
    for x in range(0, w, 416):
        for y in range(0, h, 416):
            area = (x, y, x + 416, y + 416)
            cropped_img = input.crop(area)
            rgb = pilImage.new("RGB", (416, 416), (0, 0, 0))
            rgb.paste(cropped_img, (0, 0))
            cropped_noise = attack416(rgb, noise)
            new_image.paste(cropped_noise, (x, y))
    area = (0, 0, w, h)
    output = new_image.crop(area)
    print(w, h)
    return output


# 416*416인 이미지만 공격가능
def attack416(input, noise):
    eps = 8 / 255.0
    eps_iter = 2 / 255.0
    n_iter = noise * 4
    npimg = np.asarray(input)[np.newaxis, :, :, :] / 255.0
    with graph.as_default():
        x_adv_untargeted = tog_untargeted(
            victim=detector, x_query=npimg, n_iter=n_iter, eps=eps, eps_iter=eps_iter
        )
    img = x_adv_untargeted[0] * 255
    output = pilImage.fromarray(img.astype("uint8"), "RGB")
    return output


def detect_image(input):
    frame = np.asarray(input)
    print(frame.shape)
    frame = frame[:,:,:3]
    frame = frame[:, :, ::-1] #to cv2
    h, w, c = frame.shape
    newhw = max(h//32*32+32, w//32*32+32)
    nh, nw = newhw, newhw

    new_frame = np.full((nh,nw,c), (0,0,0), dtype=np.uint8)
    new_frame[0:h, 0:w] = frame

    blob = cv2.dnn.blobFromImage(new_frame, 0.00392, (nh,nw), (0,0,0), True, crop=False)

    YOLO_net.setInput(blob)
    outs = YOLO_net.forward(output_layers)


    confidences = []
    boxes = []

    for out in outs:
        for detection in out:
            scores = detection[5:]
            class_id = np.argmax(scores)
            confidence = scores[class_id]

            if confidence > 0.4:
            # Object detected
                center_x = int(detection[0] * nw)
                center_y = int(detection[1] * nh)
                dw = int(detection[2] * nw)
                dh = int(detection[3] * nh)
                # 좌표
                x = int(center_x - dw / 2)
                y = int(center_y - dh / 2)
                boxes.append([x, y, min(dw, w-x-1), min(dh, h-y-1)])
                confidences.append(float(confidence))


        indexes = cv2.dnn.NMSBoxes(boxes, confidences, 0.5, 0.4)


    for i in range(len(boxes)):
        if i in indexes:
            x, y, w, h = boxes[i]
            #경계상자와 클래스 정보 이미지에 입력
            cv2.rectangle(new_frame, (x, y), (x + w, y + h), (0,0,255), 2)
            # print(x,y,w,h)

    h, w, c = frame.shape
    new_frame = new_frame[0:h, 0:w]

    new_frame = cv2.cvtColor(new_frame,cv2.COLOR_BGR2RGB)
    new_frame = pilImage.fromarray(new_frame)
    return new_frame




@api_view(["POST"])
def post(request):
    input_image = request.data["input_image"]
    serializer = ImageSerializer(data=request.data)
    input_img = pilImage.open(input_image)

    output = attack(input_img)
    output.save("media/adv.png")
    response = FileResponse(open("media/adv.png", "rb"))
    return response


@api_view(["POST"])
def crop(request):
    x, y, w, h, n = int(request.POST["x"].split(".")[0]), int(request.POST["y"].split(".")[0]), int(request.POST["w"].split(".")[0]), int(request.POST["h"].split(".")[0]), int(request.POST["n"])
    print(request.POST)
    x1, y1, x2, y2 = x , y, x + w, y + h
    img = pilImage.open(request.FILES["input_image"])
    img.save("media/ori.png")
    detect_image(img).save("media/before.png")

    area = (x1, y1, x2, y2)
    cropped_img = img.crop(area)
    output = attack(cropped_img, n)
    img.paste(output, area)
    img.save("media/adv.png")

    detect_image(img).save("media/after.png")

    response = FileResponse(open("media/adv.png", "rb"))
    return response

@api_view(["POST"])
def detect(request):
    img = pilImage.open(request.FILES["input_image"])
    detect_image(img).save("media/detect.png")
    response = FileResponse(open("media/detect.png", "rb"))
    return response

    



