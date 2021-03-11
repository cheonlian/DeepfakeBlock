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

@api_view(['GET'])
def index(request, id):
    image = Image.objects.get(id=id)
    serializer = ImageSerializer(image)
    return Response(serializer.data)


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
session = tf.Session(config = tf_config)


weights = 'tog/model_weights/yolo_face.h5'
detector = YOLOv3_Darknet53_Face(weights=weights)

#모든 사이즈 공격 가능
def attack(input):
    w,h = input.size
    newsize = (w+416, h+416)
    new_image = pilImage.new('RGB', newsize, (0, 0, 0))
    for x in range(0,w,416):
        for y in range(0,h,416):
            area = (x,y,x+416,y+416)
            cropped_img = input.crop(area)
            rgb = pilImage.new('RGB',(416,416),(0,0,0))
            rgb.paste(cropped_img,(0,0))
            cropped_noise = attack416(rgb)
            new_image.paste(cropped_noise, (x,y))
    area = (0,0,w, h)
    output = new_image.crop(area)
    print(w,h)
    return output

#416*416인 이미지만 공격가능
def attack416(input):
    eps = 8 / 255.       
    eps_iter = 2 / 255.  
    n_iter = 10   
    npimg = np.asarray(input)[np.newaxis, :, :, :] / 255.
    with graph.as_default():
        x_adv_untargeted = tog_untargeted(victim=detector, x_query=npimg, n_iter=n_iter, eps=eps, eps_iter=eps_iter)
    img = x_adv_untargeted[0]*255
    output = pilImage.fromarray(img.astype('uint8'), 'RGB')
    return output

@api_view(['POST'])
def post(request):
    input_image = request.data['input_image']
    serializer = ImageSerializer(data=request.data)
    input_img = pilImage.open(input_image)

    output = attack(input_img)
    output.save("media/adv.png")
    response = FileResponse(open("media/adv.png", "rb"))
    return response

@api_view(['POST'])
def crop(request):
    x1, y1, x2, y2 = int(request.data["x1"]), int(request.data["y1"]), int(request.data["x2"]), int(request.data["y2"])
    # x, y = x2 - x1, y2 - y1
    img = pilImage.open(request.data["input_image"])
    area = (x1, y1, x2, y2)
    cropped_img = img.crop((0, 0, 500, 500))
    img.paste(cropped_img, area)
    # img.show()
    output = attack(img)
    output.save("media/adv.png")
    response = FileResponse(open("media/adv.png", "rb"))
    return response