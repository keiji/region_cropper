#!/bin/python3
# -*- coding: utf-8 -*-

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import json

from entity.rect import Rect


class RegionList(object):
  generator = None
  file_name = None
  region = None

  def load(self, file_path):
    self.region = []

    with open(file_path, mode='r') as fp:
      json_object = json.load(fp)
      self.file_name = json_object['file_name']

      region_list = json_object.get('regions', None)

      if not region_list:
        return

      for c in region_list:
        rect_object = c['rect']
        rect = Rect(rect_object['left'], rect_object['top'],
                    rect_object['right'], rect_object['bottom'])

        region = Region(c['label'], c['probability'], rect)
        self.region.append(region)


class Region(object):
  label = None
  likelihood = 0.0
  rect = None

  def __init__(self, label, likelihood, rect):
    self.label = label
    self.rect = rect
    self.likelihood = likelihood

  def __lt__(self, other):
    return self.likelihood < other.likelihood
