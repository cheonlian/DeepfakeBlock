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


"""
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


def predict(input):
    eps = 8 / 255.0
    eps_iter = 2 / 255.0
    n_iter = 10
    x_query, x_meta = letterbox_image_padded(input, size=detector.model_img_size)
    with graph.as_default():
        x_adv_untargeted = tog_untargeted(
            victim=detector, x_query=x_query, n_iter=n_iter, eps=eps, eps_iter=eps_iter
        )
    img = x_adv_untargeted[0] * 255
    output = pilImage.fromarray(img.astype("uint8"), "RGB")
    return output


@api_view(["POST"])
def post(request):
    input_image = request.data["input_image"]
    serializer = ImageSerializer(data=request.data)
    input_img = pilImage.open(input_image)

    output = predict(input_img)
    output.save("media/adv.png")
    response = FileResponse(open("media/adv.png", "rb"))
    return response
"""
