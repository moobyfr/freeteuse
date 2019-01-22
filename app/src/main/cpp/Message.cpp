// Copyright (C) Yannick Le Roux.
// This file is part of Freeteuse.
//
//   Freeteuse is free software: you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation, either version 3 of the License, or
//   (at your option) any later version.
//
//   Freeteuse is distributed in the hope that it will be useful,
//   but WITHOUT ANY WARRANTY; without even the implied warranty of
//   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//   GNU General Public License for more details.
//
//   You should have received a copy of the GNU General Public License
//   along with Freeteuse.  If not, see <http://www.gnu.org/licenses/>.

#include <string.h>
#include <malloc.h>
#include "Message.hpp"

// ---------------------------------------------------
Message::Message (const char *klass)
{
  _klass = nullptr;

  if (klass)
  {
    _klass = strdup (klass);
  }
}

// ---------------------------------------------------
Message::~Message ()
{
  free (_klass);
}

// ---------------------------------------------------
bool Message::Is (const char *klass)
{
  if (_klass && klass)
  {
    return strcmp (_klass, klass) == 0;
  }

  return false;
}
