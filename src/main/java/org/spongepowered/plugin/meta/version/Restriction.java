/*
 * This file is part of plugin-meta, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.spongepowered.plugin.meta.version;

// Note: This file contains custom modifications for plugin-meta licensed under
// the MIT License.

// @formatter:off

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Describes a restriction in versioning.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class Restriction
{
    private final ArtifactVersion lowerBound;

    private final boolean lowerBoundInclusive;

    private final ArtifactVersion upperBound;

    private final boolean upperBoundInclusive;

    public static final Restriction EVERYTHING = new Restriction( null, false, null, false );

    public Restriction( ArtifactVersion lowerBound, boolean lowerBoundInclusive, ArtifactVersion upperBound,
                        boolean upperBoundInclusive )
    {
        this.lowerBound = lowerBound;
        this.lowerBoundInclusive = lowerBoundInclusive;
        this.upperBound = upperBound;
        this.upperBoundInclusive = upperBoundInclusive;
    }

    public ArtifactVersion getLowerBound()
    {
        return lowerBound;
    }

    public boolean isLowerBoundInclusive()
    {
        return lowerBoundInclusive;
    }

    public ArtifactVersion getUpperBound()
    {
        return upperBound;
    }

    public boolean isUpperBoundInclusive()
    {
        return upperBoundInclusive;
    }

    public boolean containsVersion( ArtifactVersion version )
    {
        if ( lowerBound != null )
        {
            int comparison = lowerBound.compareTo( version );

            if ( ( comparison == 0 ) && !lowerBoundInclusive )
            {
                return false;
            }
            if ( comparison > 0 )
            {
                return false;
            }
        }
        if ( upperBound != null )
        {
            int comparison = upperBound.compareTo( version );

            if ( ( comparison == 0 ) && !upperBoundInclusive )
            {
                return false;
            }
            if ( comparison < 0 )
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = 13;

        if ( lowerBound == null )
        {
            result += 1;
        }
        else
        {
            result += lowerBound.hashCode();
        }

        result *= lowerBoundInclusive ? 1 : 2;

        if ( upperBound == null )
        {
            result -= 3;
        }
        else
        {
            result -= upperBound.hashCode();
        }

        result *= upperBoundInclusive ? 2 : 3;

        return result;
    }

    @Override
    public boolean equals( Object other )
    {
        if ( this == other )
        {
            return true;
        }

        if ( !( other instanceof Restriction ) )
        {
            return false;
        }

        Restriction restriction = (Restriction) other;
        if ( lowerBound != null )
        {
            if ( !lowerBound.equals( restriction.lowerBound ) )
            {
                return false;
            }
        }
        else if ( restriction.lowerBound != null )
        {
            return false;
        }

        if ( lowerBoundInclusive != restriction.lowerBoundInclusive )
        {
            return false;
        }

        if ( upperBound != null )
        {
            if ( !upperBound.equals( restriction.upperBound ) )
            {
                return false;
            }
        }
        else if ( restriction.upperBound != null )
        {
            return false;
        }

        return upperBoundInclusive == restriction.upperBoundInclusive;

    }

    public String toString()
    {
        StringBuilder buf = new StringBuilder();

        buf.append( isLowerBoundInclusive() ? '[' : '(' );
        if ( getLowerBound() != null )
        {
            buf.append( getLowerBound().toString() );
        }
        buf.append( ',' );
        if ( getUpperBound() != null )
        {
            buf.append( getUpperBound().toString() );
        }
        buf.append( isUpperBoundInclusive() ? ']' : ')' );

        return buf.toString();
    }
}
