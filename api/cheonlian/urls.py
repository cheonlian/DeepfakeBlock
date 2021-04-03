"""cheonlian URL Configuration

The `urlpatterns` list routes URLs to views. For more information please see:
    https://docs.djangoproject.com/en/3.1/topics/http/urls/
Examples:
Function views
    1. Add an import:  from my_app import views
    2. Add a URL to urlpatterns:  path('', views.home, name='home')
Class-based views
    1. Add an import:  from other_app.views import Home
    2. Add a URL to urlpatterns:  path('', Home.as_view(), name='home')
Including another URLconf
    1. Import the include() function: from django.urls import include, path
    2. Add a URL to urlpatterns:  path('blog/', include('blog.urls'))
"""

from django.contrib import admin
from django.urls import path, include
from api import views as api_views
from django.conf.urls.static import static
from django.conf import settings
from Web import views as web_views

urlpatterns = [
    path("admin/", admin.site.urls),

    # api views
    path("<int:id>/", api_views.index),
    path("post/", api_views.post),
    path("crop/", api_views.crop),
    path("detect/", api_views.detect),

    # web_views
    path("", web_views.index, name="web-main"),
    path("upload/", web_views.upload_view),
] + static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
# urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
