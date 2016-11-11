#!/bin/python3
# -*- coding: utf-8 -*-

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import os
import shutil
import sys

import gflags
from PIL import Image, ImageDraw

from common import convertRgb, save_file, get_file_list
from entity.region_list import RegionList

FLAGS = gflags.FLAGS
gflags.DEFINE_string('source_dir', None, '処理するディレクトリ')
gflags.DEFINE_string('output_dir', None, '出力するディレクトリ')


def _crop_and_save_candidates(image_path, region_list, base_dir):
  cropped_count = 0

  name, ext = os.path.splitext(os.path.basename(image_path))

  with Image.open(image_path) as image:
    for index, region in enumerate(region_list):
      output_dir = os.path.join(base_dir, ('label_%d' % region.label))
      if not os.path.exists(output_dir):
        os.makedirs(output_dir)

      rect = region.rect
      img = image.crop((rect.left, rect.top, rect.right, rect.bottom))

      img = convertRgb(img)
      file_name = '%s-%d.jpg' % (name, (index + 1))
      save_file(img, output_dir, file_name)
      cropped_count += 1

  return cropped_count


def _process(file_list, output_dir, debug_flag=False):
  treated_files = 0
  cropped_count = 0

  size = len(file_list)

  for file_path in file_list:
    treated_files += 1
    if treated_files % 100 == 0:
      print("Processing %d/%d - %.1f%%" % (treated_files, size, (treated_files / size) * 100))

    dir = os.path.dirname(file_path)

    region_list = RegionList()
    region_list.load(file_path)

    image_path = os.path.join(dir, region_list.file_name)

    if len(region_list.region) == 0:
      print('File %s has no rect.' % file_path)
      continue

    if not os.path.exists(image_path):
      print('***%s is not found.' % image_path)
      continue

    if debug_flag:
      print('Processing... %s' % image_path)

    cropped_count += _crop_and_save_candidates(image_path, region_list.region, output_dir)

  return treated_files, cropped_count


def main(argv=None):
  try:
    FLAGS(argv)
  except gflags.FlagsError as e:
    pass

  assert FLAGS.source_dir, '--source_dir is not defined.'
  assert FLAGS.output_dir, '--output_dir is not defined.'

  assert os.path.exists(FLAGS.source_dir), '%s is not exist.' % FLAGS.source_dir
  assert os.path.exists(FLAGS.output_dir), '%s is not exist.' % FLAGS.output_dir

  file_list = get_file_list(FLAGS.source_dir)

  output_dir_file_list = os.listdir(FLAGS.output_dir)
  if output_dir_file_list:
    label_dir_list = filter(lambda f: f.startswith('label'), output_dir_file_list)
    for label_dir in label_dir_list:
      print('dir %s will be removed.' % label_dir)
      shutil.rmtree(os.path.join(FLAGS.output_dir, label_dir))

  treated_files, cropped_count = _process(file_list, FLAGS.output_dir,
                                          debug_flag=False)

  print('Processed: %d, Cropped regions: %d' % (treated_files, cropped_count))


if __name__ == '__main__':
  main(sys.argv)
