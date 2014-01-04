/*
// Copyright (C) 2005 by Carlos Castellanos
// 
// This program is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the
// Free Software Foundation; either version 2 of the License, or (at your
// option) any later version.
// 
// This program is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
 */

/**
 * 
 */

/**
 * @author carlos
 *
 */

package cc.bodyd;

import java.util.EventListener;

public interface BodyDEventListener extends EventListener
{
	public abstract void bodyDEvent(BodyD bd);
}
