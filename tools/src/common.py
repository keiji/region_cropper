#!/bin/python3
# -*- coding: utf-8 -*-

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import os
import time

from PIL import Image


def get_file_list(dir):
  result_list = []

  file_list = os.listdir(dir)
  file_list = map(lambda f: os.path.join(dir, f), file_list)
  file_list = filter(lambda f: os.path.isdir(f)
                               or (f.lower().endswith(".json")
                                   and not "-candidate" in f.lower()),
                     file_list)

  for file in file_list:
    if os.path.isdir(file):
      result_list.extend(get_file_list(file))
      continue

    result_list.append(file)

  return result_list


def convertRgb(img):
  splitted_image = img.split()
  if len(splitted_image) <= 3:
    return img.convert('RGB')

  # http://stackoverflow.com/questions/9166400/convert-rgba-png-to-rgb-with-pil
  rgb_image = Image.new("RGB", img.size, (255, 255, 255))
  rgb_image.paste(img, mask=splitted_image[3])  # 3 is the alpha channel
  return rgb_image


def save_file(img, output_dir, file_name):
  file_path = os.path.join(output_dir, file_name)
  img.save(file_path, 'JPEG', quality=80)


def __current_time_in_millis():
  return int(round(time.time() * 1000))
