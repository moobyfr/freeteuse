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

extern "C"
{
#include <foils/hid.h>
}

#include "Log.hpp"
#include "Key.hpp"

// ---------------------------------------------------
Key::Key (uint32_t command,
          uint8_t  report,
          uint32_t code)
{
  _command = command;
  _report  = report;
  _code    = code;
}

// ---------------------------------------------------
Key::~Key ()
{
}

// ---------------------------------------------------
bool Key::Is (const uint32_t command)
{
  return (_command == command);
}

// ---------------------------------------------------
void Key::Dump (const char *text)
{
  LOGI ("%s ==> [%x:%x]", text, _report, _code);
}

// ---------------------------------------------------
void Key::Press (struct foils_hid *hid_client)
{
  Dump ("PRESS");
  ReportSend (hid_client,
              _code);
}

// ---------------------------------------------------
void Key::Release (struct foils_hid *hid_client)
{
  uint32_t zero = 0;

  Dump ("RELEASE");
  ReportSend (hid_client,
              zero);
}

// ---------------------------------------------------
void Key::ReportSend (struct foils_hid *hid_client,
                      uint32_t          code)
{
#if 0
  uint16_t code16 = (uint16_t) code;

  foils_hid_input_report_send (hid_client,
                               DEVICE_INDEX,
                               _report,
                               1,
                               &code16, sizeof (code16));
#endif
}
