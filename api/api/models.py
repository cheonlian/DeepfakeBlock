from django.db import models

class Image(models.Model):
    title = models.CharField(max_length=30)
    input_image = models.ImageField()
    output_image = models.ImageField()
