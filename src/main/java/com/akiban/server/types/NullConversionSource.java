/**
 * Copyright (C) 2011 Akiban Technologies Inc.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package com.akiban.server.types;

public final class NullConversionSource implements ConversionSource {

    public static ConversionSource only() {
        return INSTANCE;
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public long getLong() {
        throw new SourceIsNullException();
    }

    @Override
    public double getDouble() {
        throw new SourceIsNullException();
    }

    @Override
    public <T> T getObject(Class<T> requiredClass) {
        throw new SourceIsNullException();
    }

    @Override
    public AkType conversionType() {
        return AkType.NULL;
    }

    private NullConversionSource() {}

    private static final NullConversionSource INSTANCE = new NullConversionSource();
}
