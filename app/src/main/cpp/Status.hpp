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

#pragma once

#include <stdint.h>
extern "C"
{
#include <foils/hid.h>
}
#include "Message.hpp"

class Status : public Message
{
  public:
    Status (enum foils_hid_state code);

    virtual ~Status ();

    enum foils_hid_state GetCode ();

  private:
    enum foils_hid_state _code;
};
