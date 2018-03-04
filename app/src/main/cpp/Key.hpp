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

#include <stdio.h>
#include "Message.hpp"

struct foils_hid;

class Key : public Message
{
  public:
    Key (uint32_t command,
         uint8_t  report,
         uint32_t code);

    virtual ~Key ();

    bool Is (const uint32_t command);

    void Dump (const char *text);

    void Press (struct foils_hid *hid_client);

    void Release (struct foils_hid *hid_client);

  private:
    static const size_t DEVICE_INDEX = 0;

    uint32_t _command;
    uint8_t  _report;
    uint32_t _code;

    void ReportSend (struct foils_hid *hid_client,
                     uint32_t          code);
};
