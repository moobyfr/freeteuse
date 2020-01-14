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

#include <cstdio>
#include <unistd.h>
#include <cstdlib>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <fcntl.h>
#include <cstring>

#include "Log.hpp"
#include "Rcu.hpp"
#include "Status.hpp"

#pragma clang diagnostic push
#pragma ide   diagnostic ignored "OCUnusedMacroInspection"
#pragma clang diagnostic ignored "-Wunused-parameter"

Rcu *Rcu::_current_rcu = nullptr;

// ---------------------------------------------------
static const uint8_t rcu_report_descriptor[] =
{
  0x05, 0x0C,  // Usage Page (Consumer)
  0x09, 0x01,  // Usage (Consumer Control)
    // TV keys
    0xA1, 0x01,             // Collection (Application)
    0x85, 0x01,             //   Report ID (1)
    0x95, 0x01,             //   Report Count (1)
    0x75, 0x20,             //   Report Size (32)
    0x19, 0x00,             //   Usage Minimum (Consumer Control)
    0x2A, 0x9C, 0x02,       //   Usage Maximum (AC Distribute Vertically)
    0x15, 0x00,             //   Logical Minimum (1)
    0x26, 0x8C, 0x02,       //   Logical Maximum (652)
    0x80,                   //   Input
    0xC0,                   // End Collection


  0x05, 0x01,  // Usage Page (Desktop)
  0x09, 0x06,  // Usage (Keyboard)
    // Navigation keys
    0xA1, 0x01,             // Collection (Application)
    0x85, 0x02,             //   Report ID (2)
    0x05, 0x07,             //   Usage Page (Keyboard)
    0x95, 0x01,             //   Report Count (1)
    0x75, 0x20,             //   Report Size (32)
    0x19, 0x00,             //   Usage Minimum (None)
    0x2A, 0xFF, 0x00,       //   Usage Maximum (FFh)
    0x15, 0x00,             //   Logical Minimum (0)
    0x26, 0xFF, 0x00,       //   Logical Maximum (255)
    0x80,                   //   Input
    0xC0,                   // End Collection

    // Unicode
    0xA1, 0x01,             // Collection (Application)
    0x85, 0x03,             //   Report ID (3)
    0x05, 0x10,             //   Usage Page (Unicode)
    0x08,                   //   Usage (00h)
    0x95, 0x01,             //   Report Count (1)
    0x75, 0x20,             //   Report Size (32)
    0x14,                   //   Logical Minimum (0)
    0x27, 0xFF, 0xFF, 0xFF, //   Logical Maximum (2**24-1)
    0x81, 0x62,             //   Input (Variable, No pref state, No Null Pos)
    0xC0                    // End Collection
};

// ---------------------------------------------------
static const struct foils_hid_device_descriptor descriptors[] =
{
  { "RCU",       // name
    0x0100,      // version
    (void*) rcu_report_descriptor, sizeof (rcu_report_descriptor), // report descriptor
    nullptr, 0,  // physical descriptor
    nullptr, 0   // strings descriptor
  }
};

// ---------------------------------------------------
const struct foils_hid_handler Rcu::handler =
{
  .status                  = Rcu::OnStatus,
  .feature_report          = (void (*) (foils_hid *, uint32_t, uint8_t, const void *, size_t)) Rcu::Stub,
  .output_report           = (void (*) (foils_hid *, uint32_t, uint8_t, const void *, size_t)) Rcu::Stub,
  .feature_report_sollicit = (void (*) (foils_hid *, uint32_t, uint8_t)) Rcu::Stub
};

// ---------------------------------------------------
void Rcu::OnStatus (struct foils_hid       *client,
                    enum   foils_hid_state  state)
{
  if (_current_rcu)
  {
    Status *status = new Status(state);

    _current_rcu->_status_pipe->Write(status);
  }
}

// ---------------------------------------------------
void Rcu::Stub ()
{
  LOGI ("Stub");
}

// ---------------------------------------------------
void Rcu::OnKeyAvailable (struct ela_event_source *source,
                          int                      fd,
                          uint32_t                 mask,
                          Rcu                     *rcu)
{
  Message *message = rcu->_looper_pipe->Read ();

  if (message)
  {
    Key *key = dynamic_cast<Key *> (message);

    if (message->Is ("CLOSE_PIPE"))
    {
      ela_exit (rcu->_looper);
    }

    // Release previous key
    if (rcu->_pending_release)
    {
      OnKeyRelease (rcu->_key_release_trigger,
                    0,
                    0,
                    rcu);
    }

    if (key)
    {
      if (key->Is (KEY_RELEASE))
      {
        key->Release (rcu->_hid_client);
      }
      else
      {
        key->Press (rcu->_hid_client);

        // Schedule key release
        if (key->Is (KEY_PRESS_RELEASE))
        {
          rcu->_pending_release = key;

          ela_add (rcu->_looper,
                   rcu->_key_release_trigger);
          return;
        }
      }
    }

    delete message;
  }
}

// ---------------------------------------------------
void Rcu::OnKeyRelease (struct ela_event_source *source,
                        int                      fd,
                        uint32_t                 mask,
                        Rcu                     *rcu)
{
  if (rcu->_pending_release)
  {
    rcu->_pending_release->Release (rcu->_hid_client);

    ela_remove (rcu->_looper,
                source);

    delete (rcu->_pending_release);
    rcu->_pending_release = nullptr;
  }
}

// ---------------------------------------------------
void Rcu::SendKeyPress (uint8_t  report_id,
                        uint32_t key_code,
                        bool     with_release)
{
  if (_key_press_trigger)
  {
    Key *key;

    if (with_release)
    {
      key = new Key (KEY_PRESS_RELEASE,
                     report_id,
                     key_code);
    }
    else
    {
      key = new Key (KEY_PRESS,
                     report_id,
                     key_code);
    }

    _looper_pipe->Write (key);
  }
}

// ---------------------------------------------------
void Rcu::SendKeyRelease (uint8_t  report_id,
                          uint32_t key_code)
{
  if (_key_press_trigger)
  {
    Key *key = new Key (KEY_RELEASE,
                        report_id,
                        key_code);

    _looper_pipe->Write (key);
  }
}

// ---------------------------------------------------
const char *Rcu::ReadStatus ()
{
  const char *result  = nullptr;
  Message    *message = _status_pipe->Read ();

  if (message)
  {
    if (message->Is ("CLOSE_PIPE"))
    {
      result = "EXIT";
    }
    else
    {
      Status *status = dynamic_cast<Status *> (message);

      switch (status->GetCode ())
      {
        case FOILS_HID_IDLE:
          result = "idle";
          break;

        case FOILS_HID_CONNECTING:
          result = "connecting";
          break;

        case FOILS_HID_CONNECTED:
          result = "connected";
          break;

        case FOILS_HID_RESOLVE_FAILED:
          result = "resolve failed";
          break;

        case FOILS_HID_DROPPED:
          result = "dropped";
          break;
      }
    }

    delete message;
  }

  return result;
}

// ---------------------------------------------------
Rcu::Rcu ()
{
}

// ---------------------------------------------------
int Rcu::GetFamilly (const char *address)
{
  struct addrinfo  hints;
  struct addrinfo *res;
  int              error;
  int              familly;

  memset (&hints, 0, sizeof (hints));
  hints.ai_family = PF_UNSPEC;
  hints.ai_flags  = AI_NUMERICHOST;

  error = getaddrinfo (address, nullptr, &hints, &res);
  if (error)
  {
    LOGE ("Rcu::GetFamilly: %s", gai_strerror (error));
    return AF_UNSPEC;
  }

  familly = res->ai_family;
  freeaddrinfo (res);

  return familly;
}

// ---------------------------------------------------
void Rcu::Connect (const char *address,
                   uint16_t    port)
{
  unsigned char addr[sizeof (struct in6_addr)];
  int           familly = GetFamilly (address);

  LOGD ("Rcu::Connect (%s, %d)", address, port);

  if (   (familly != AF_UNSPEC)
      && inet_pton (familly, address, &addr))
  {
    _current_rcu = this;

    _looper_pipe = new Pipe ();
    _status_pipe = new Pipe ();

    _looper = ela_create (nullptr);

    // Key press
    {
      ela_source_alloc (_looper,
                        (void (*) (ela_event_source *, int, uint32_t, void *)) OnKeyAvailable,
                        this,
                        &_key_press_trigger);
      ela_set_fd (_looper,
                  _key_press_trigger,
                  _looper_pipe->GetReadEnd (),
                  ELA_EVENT_READABLE);
      ela_add (_looper,
               _key_press_trigger);
    }

    // Key release
    {
      struct timeval timeout = {0, 100000};

      ela_source_alloc (_looper,
                        (void (*) (ela_event_source *, int, uint32_t, void *)) OnKeyRelease,
                        this,
                        &_key_release_trigger);
      ela_set_timeout (_looper,
                       _key_release_trigger,
                       &timeout,
                       ELA_EVENT_ONCE);
    }

    // hid connection
    {
      _hid_client = (foils_hid *) malloc (sizeof (foils_hid));

      {
        int err = foils_hid_init (_hid_client,
                                  _looper,
                                  &handler,
                                  descriptors,
                                  1);
        if (err)
        {
          LOGE ("Rcu::Rcu: %s", strerror (err));
        }
      }

      if (familly == AF_INET)
      {
        LOGI ("IPv4");
        foils_hid_client_connect_ipv4 (_hid_client,
                                       (const in_addr *) &addr,
                                       port);
      }
      else if (familly == AF_INET6)
      {
        LOGI ("IPv6");
        foils_hid_client_connect_ipv6 (_hid_client,
                                       (const in6_addr *) &addr,
                                       port);
      }

      foils_hid_device_enable (_hid_client,
                               0);
    }

    pthread_create (&_looper_thread,
                    nullptr,
                    (void *(*) (void *)) ela_run,
                    _looper);
  }
}

// ---------------------------------------------------
void Rcu::Disconnect ()
{
  if (_key_press_trigger)
  {
    _looper_pipe->Write (new Message ("CLOSE_PIPE"));
    pthread_join (_looper_thread, nullptr);
    delete _looper_pipe;
    _looper_pipe = nullptr;

    _status_pipe->Write (new Message ("CLOSE_PIPE"));

    ela_remove      (_looper, _key_release_trigger);
    ela_source_free (_looper, _key_release_trigger);
    _key_release_trigger = nullptr;

    ela_remove      (_looper, _key_press_trigger);
    ela_source_free (_looper, _key_press_trigger);
    _key_press_trigger = nullptr;
  }

  foils_hid_deinit (_hid_client);
  free (_hid_client);
  _hid_client = nullptr;

  ela_close (_looper);
  _looper = nullptr;

  if (_pending_release)
  {
    delete (_pending_release);
    _pending_release = nullptr;
  }

  _current_rcu = nullptr;
}


// ---------------------------------------------------
Rcu::~Rcu ()
{
  delete _status_pipe;
}

#pragma clang diagnostic pop
