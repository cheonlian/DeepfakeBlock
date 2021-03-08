from django.shortcuts import render, HttpResponse
from django.http import FileResponse
from .serializers import ImageSerializer

from django.views import View
from .models import Image

from django.utils.decorators import method_decorator
from django.views.decorators.csrf import csrf_exempt
from rest_framework.decorators import api_view
from rest_framework.response import Response

@api_view(['GET'])
def index(request, title):
    # img = open('media/iu_ori.png', 'rb')
    # response = FileResponse(img)
    image = Image.objects.filter(title = title)
    serializer = ImageSerializer(image)
    return Response(serializer.data)


@api_view(['POST'])
def post(request):
    serializer = ImageSerializer(data=request.data)
    if serializer.is_valid(raise_exception=True):
        serializer.save()
        return Response(serializer.data, status=status.HTTP_201_CREATED)

