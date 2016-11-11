#!/bin/python3
# -*- coding: utf-8 -*-

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function


class Rect(object):
  left = 0
  top = 0
  right = 0
  bottom = 0

  def width(self):
    return (self.right - self.left)

  def height(self):
    return (self.bottom - self.top)

  def center(self):
    cX = round(self.left + (self.width() / 2))
    cY = round(self.top + (self.height() / 2))

    return (cX, cY)

  def __init__(self, left, top, right, bottom):
    self.left = left
    self.top = top
    self.right = right
    self.bottom = bottom

  def __eq__(self, other):
    if isinstance(other, Rect):
      return (
        (self.left == other.left)
        and (self.top == other.top)
        and (self.right == other.right)
        and (self.bottom == other.bottom)
      )
    else:
      return False

  def __ne__(self, other):
    return (not self.__eq__(other))

  def __repr__(self):
    return "Entry(%f, %f, %f, %f)" % (
      self.left, self.top, self.right, self.bottom)

  def __hash__(self):
    return hash(self.__repr__())

  def copy(self):
    return Rect(self.left, self.top, self.right, self.bottom)

  def tostring(self):
    return '(%f, %f, %f, %f)' % (self.left, self.top, self.right, self.bottom)
