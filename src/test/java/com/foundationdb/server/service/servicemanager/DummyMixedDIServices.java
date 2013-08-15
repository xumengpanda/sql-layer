/**
 * Copyright (C) 2009-2013 Akiban Technologies, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.foundationdb.server.service.servicemanager;

import javax.inject.Inject;

/**
 * Alpha <- Beta <- Gamma <- nothing
 */
final class DummyMixedDIServices {

    public static class MixedDIAlpha implements DummyInterfaces.Alpha {
        @Inject private DummyInterfaces.Beta beta = null;
        @Override
        public void start() {
            assert beta != null;
        }

        @Override
        public void stop() {}
    }

    public static class MixedDIBeta implements DummyInterfaces.Beta {
        @Override
        public void start() {
            if (this == GuicerTest.onlyGuicer().get(DummyInterfaces.Gamma.class, GuicerTest.MESSAGING_ACTIONS)) {
                throw new Error("how can this instance be equal to an instance of another class?!");
            }
        }

        @Override
        public void stop() {}
    }

    public static class MixedDIGamma implements DummyInterfaces.Gamma {
        @Override
        public void start() {}

        @Override
        public void stop() {}
    }
}