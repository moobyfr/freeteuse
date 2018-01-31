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

#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <fcntl.h>

#include "Log.hpp"
#include "Rcu.hpp"

#pragma clang diagnostic push
#pragma ide   diagnostic ignored "OCUnusedMacroInspection"
#pragma clang diagnostic ignored "-Wunused-parameter"

// ---------------------------------------------------
static const uint8_t rcu_report_descriptor[] =
{
  0x05, 0x0C,       // Usage Page (Consumer)
  0x09, 0x01,       // Usage (Consumer Control)
  0xA1, 0x01,       // Collection (Application)
  0x85, 0x01,       //   Report ID (1)
  0x95, 0x01,       //   Report Count (1)
  0x75, 0x10,       //   Report Size (16)
  0x19, 0x00,       //   Usage Minimum (Consumer Control)
  0x2A, 0x9C, 0x02, //   Usage Maximum (AC Distribute Vertically)
  0x15, 0x00,       //   Logical Minimum (1)
  0x26, 0x8C, 0x02, //   Logical Maximum (652)
  0x80,             //   Input
  0xC0,             // End Collection

  0x05, 0x01,       // Usage Page (Desktop)
  0x09, 0x06,       // Usage (Keyboard)
  0xA1, 0x01,       // Collection (Application)
  0x85, 0x02,       //   Report ID (2)
  0x95, 0x01,       //   Report Count (1)
  0x75, 0x10,       //   Report Size (16)
  0x15, 0x00,       //   Logical Minimum (0)
  0x26, 0xFF, 0x00, //   Logical Maximum (255)
  0x05, 0x07,       //   Usage Page (Keyboard)
  0x19, 0x00,       //   Usage Minimum (None)
  0x2A, 0xFF, 0x00, //   Usage Maximum (FFh)
  0x80,             //   Input
  0xC0              // End Collection
};

// ---------------------------------------------------
static const struct foils_hid_device_descriptor descriptors[] =
{
  { "RCU",     // name
    0x0100,    // version
    (void*) rcu_report_descriptor, sizeof (rcu_report_descriptor), // report descriptor
    NULL, 0,  // physical descriptor
    NULL, 0   // strings descriptor
  }
};

// ---------------------------------------------------
const struct foils_hid_handler Rcu::handler =
{
  .status                  = Rcu::OnStatus,
  .feature_report          = Rcu::OnFeatureReport,
  .output_report           = Rcu::OnOutputReport,
  .feature_report_sollicit = Rcu::OnFeatureReportSollicit
};

// ---------------------------------------------------
void Rcu::OnStatus (struct foils_hid       *client,
                    enum   foils_hid_state  state)
{
  const char *st = NULL;

  switch (state)
  {
    case FOILS_HID_IDLE:
    st = "idle";
    break;
    case FOILS_HID_CONNECTING:
    st = "connecting";
    break;
    case FOILS_HID_CONNECTED:
    st = "connected";
    break;
    case FOILS_HID_RESOLVE_FAILED:
    st = "resolve failed";
    break;
    case FOILS_HID_DROPPED:
    st = "dropped";
    break;
  }

  LOGI ("Status ===>> %s", st);
}

// ---------------------------------------------------
void Rcu::OnFeatureReport (struct foils_hid *client,
                           uint32_t          device_id,
                           uint8_t           report_id,
                           const void       *data,
                           size_t            datalen)
{
  LOGI ("OnFeatureReport");
}

// ---------------------------------------------------
void Rcu::OnOutputReport (struct foils_hid *client,
                          uint32_t          device_id,
                          uint8_t           report_id,
                          const void       *data,
                          size_t            datalen)
{
  LOGI ("OnOutputReport");
}

// ---------------------------------------------------
void Rcu::OnFeatureReportSollicit (struct foils_hid *client,
                                   uint32_t          device_id,
                                   uint8_t           report_id)
{
  LOGI ("OnFeatureReportSollicit");
}

// ---------------------------------------------------
void Rcu::OnKeyAvailable (struct ela_event_source *source,
                          int                      fd,
                          uint32_t                 mask,
                          Rcu                     *rcu)
{
  Key *key;

  if (read (rcu->_looper_pipe_rfd, &key, sizeof (Key *)) == -1)
  {
    LOGE ("Rcu::OnKeyAvailable: %s", strerror (errno));
  }
  else
  {
    // Release previous key
    if (rcu->_pending_release)
    {
      OnKeyRelease (rcu->_key_release_trigger,
                    0,
                    0,
                    rcu);
    }

    if (key->Is (EXIT_LOOPER_CODE))
    {
      ela_exit (rcu->_looper);
    }
    else if (key->Is (KEY_RELEASE))
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
    delete key;
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
    rcu->_pending_release = NULL;
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

    if (write (_looper_pipe_wfd, &key, sizeof (Key *)) == -1)
    {
      LOGE ("Rcu::SendKeyPress: %s", strerror (errno));
    }
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

    if (write (_looper_pipe_wfd, &key, sizeof (Key *)) == -1)
    {
      LOGE ("Rcu::SendKeyPress: %s", strerror (errno));
    }
  }
}

// ---------------------------------------------------
void Rcu::SendControlKey (uint32_t key_code)
{
  Key *key = new Key (key_code);

  if (write (_looper_pipe_wfd, &key, sizeof (Key *)) == -1)
  {
    LOGE ("Rcu::SendControlKey: %s", strerror (errno));
  }
}

// ---------------------------------------------------
Rcu::Rcu ()
{
  _pending_release     = NULL;
  _key_press_trigger   = NULL;
  _key_release_trigger = NULL;

  _looper = ela_create (NULL);

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

  error = getaddrinfo (address, NULL, &hints, &res);
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
    // Pipe
    {
      int fd[2];

      if (pipe (fd) < 0)
      {
        LOGE ("Rcu::Connect: %s", strerror (errno));
        return;
      }

      _looper_pipe_rfd = fd[0];
      _looper_pipe_wfd = fd[1];
    }

    // Key press
    {
      ela_source_alloc (_looper,
                        (void (*) (ela_event_source *, int, uint32_t, void *)) OnKeyAvailable,
                        this,
                        &_key_press_trigger);
      ela_set_fd (_looper,
                  _key_press_trigger,
                  _looper_pipe_rfd,
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
                    NULL,
                    (void *(*) (void *)) ela_run,
                    _looper);
  }
}

// ---------------------------------------------------
Rcu::~Rcu ()
{
  if (_key_press_trigger)
  {
    SendControlKey (EXIT_LOOPER_CODE);
    pthread_join (_looper_thread,
                  NULL);

    close (_looper_pipe_wfd);
    close (_looper_pipe_rfd);

    ela_remove      (_looper, _key_release_trigger);
    ela_source_free (_looper, _key_release_trigger);

    ela_remove      (_looper, _key_press_trigger);
    ela_source_free (_looper, _key_press_trigger);
  }

  foils_hid_deinit (_hid_client);
  free (_hid_client);

  ela_close (_looper);

  if (_pending_release)
  {
    delete (_pending_release);
  }
}

#pragma clang diagnostic pop
