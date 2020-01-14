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

#include <unistd.h>
#include <cstring>
#include <cerrno>
#include "Pipe.hpp"
#include "Log.hpp"

// ---------------------------------------------------
Pipe::Pipe ()
{
  int fd[2];

  if (pipe (fd) < 0)
  {
    LOGE ("Rcu::Connect: %s", strerror (errno));
    return;
  }

  _read_fd  = fd[0];
  _write_fd = fd[1];
}

// ---------------------------------------------------
Pipe::~Pipe ()
{
  close (_read_fd);
  close (_write_fd);
}

// ---------------------------------------------------
int Pipe::GetReadEnd ()
{
  return _read_fd;
}

// ---------------------------------------------------
Message *Pipe::Read ()
{
  Message *message;

  if (read (_read_fd, &message, sizeof (Message *)) == -1)
  {
    LOGE ("Pipe::Read: %s", strerror (errno));
    return nullptr;
  }

  return message;
}

// ---------------------------------------------------
void Pipe::Write (Message *message)
{
  if (write (_write_fd, &message, sizeof (Message *)) == -1)
  {
    LOGE ("Pipe::Write: %s", strerror (errno));
  }
}
