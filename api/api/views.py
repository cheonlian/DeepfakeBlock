from django.shortcuts import render, HttpResponse
from django.http import FileResponse
from .serializers import ImageSerializer

from django.views import View
from .models import Image

from django.utils.decorators import method_decorator
from django.views.decorators.csrf import csrf_exempt
from rest_framework.decorators import api_view
from rest_framework.response import Response
from rest_framework import status


@api_view(["GET"])
def index(request, id):
    # img = open('media/iu_ori.png', 'rb')
    # response = FileResponse(img)
    print("###1")
    image = Image.objects.get(id=id)
    print("###2")
    print(image)
    serializer = ImageSerializer(image)
    print("###3")
    print(serializer)
    print(serializer.data)
    return Response(serializer.data)


@api_view(["POST"])
def post(request):
    input_image = request.data["input_image"]
    print(type(input_image))
    serializer = ImageSerializer(data=request.data)
    if serializer.is_valid(raise_exception=True):
        serializer.save()
        return Response(serializer.data, status=status.HTTP_201_CREATED)
