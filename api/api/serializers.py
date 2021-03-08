from api.models import Image
from rest_framework import serializers


class ImageSerializer(serializers.HyperlinkedModelSerializer):

    class Meta:
        model = Image
        fields = ('title', 'input_image')