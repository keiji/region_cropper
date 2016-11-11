#!/bin/python3
# -*- coding: utf-8 -*-

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import os
import sys

import gflags
from PIL import Image, ImageDraw

from common import get_file_list
from entity.region_list import RegionList

FLAGS = gflags.FLAGS
gflags.DEFINE_string('source_dir', None, '処理するディレクトリ')
gflags.DEFINE_string('output_dir', None, '出力するディレクトリ')
gflags.DEFINE_list('remove_labels', [], '取り除くラベルをリスト形式 0,1,2 で指定する')


def _save_candidate_region_image(image_path, region_list, remove_labels, output_dir):
  if not os.path.exists(output_dir):
    os.mkdir(output_dir)

  name, ext = os.path.splitext(os.path.basename(image_path))

  with Image.open(image_path) as image:
    image = image.convert('RGBA')
    draw = ImageDraw.Draw(image, mode='RGBA')

    for region in region_list:
      if not region.label in remove_labels:
        continue
      rect = region.rect
      draw.rectangle((rect.left, rect.top, rect.right, rect.bottom), fill=(0xff, 0xff, 0xff))

    file_name = '%s_.jpg' % name
    image.save(os.path.join(output_dir, file_name), 'JPEG', quality=80)


def _process(file_list, remove_labels, output_dir, debug_flag=False):
  treated_files = 0

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

    _save_candidate_region_image(image_path, region_list.region, remove_labels, output_dir)

  return treated_files


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

  remove_labels = list(map(lambda str: int(str), FLAGS.remove_labels))
  treated_files = _process(file_list, remove_labels, FLAGS.output_dir, debug_flag=False)

  print('Processed: %d' % (treated_files))


if __name__ == '__main__':
  main(sys.argv)
