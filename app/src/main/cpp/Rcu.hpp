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

#include <pthread.h>

#include "Key.hpp"
#include "Pipe.hpp"

extern "C"
{
#include <foils/hid.h>
}

class Rcu
{
  public:
    Rcu ();

    ~Rcu ();

    void Connect (const char *address,
                  uint16_t    port);

    void SendKeyPress (uint8_t  report_id,
                       uint32_t code,
                       bool     with_release);

    void SendKeyRelease (uint8_t  report_id,
                         uint32_t key_code);

    const char *ReadStatus ();

  private:
    typedef enum
    {
      KEY_PRESS,
      KEY_PRESS_RELEASE,
      KEY_RELEASE
    } Command;

    static Rcu *_current_rcu;

    pthread_t                _looper_thread;
    struct ela_el           *_looper;
    struct foils_hid        *_hid_client;
    Key                     *_pending_release;
    Pipe                    *_looper_pipe;
    Pipe                    *_status_pipe;
    struct ela_event_source *_key_press_trigger;
    struct ela_event_source *_key_release_trigger;

  private:
    int GetFamilly (const char *address);

  private:
    static const struct foils_hid_handler handler;

    static void OnStatus (struct foils_hid     *client,
                          enum foils_hid_state  state);

    static void Stub ();

    static void OnKeyAvailable (struct ela_event_source *source,
                                int                      fd,
                                uint32_t                 mask,
                                Rcu                     *rcu);

    static void OnKeyRelease (struct ela_event_source *source,
                              int                      fd,
                              uint32_t                 mask,
                              Rcu                     *rcu);
};
