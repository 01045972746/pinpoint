/*
 * Copyright 2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.profiler.monitor.metric;

import com.navercorp.pinpoint.common.ServiceType;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author emeroad
 */
public class DynamicAcceptHistogram implements AcceptHistogram {

    private final ConcurrentMap<ResponseKey, Histogram> map;

    public DynamicAcceptHistogram() {
        this.map = new ConcurrentHashMap<ResponseKey, Histogram>();
    }

    @Override
    public boolean addResponseTime(String parentApplicationName, short serviceTypeCode, int millis) {
        if (parentApplicationName == null) {
            throw new NullPointerException("parentApplicationName must not be null");
        }
        // ServiceType object로 변경하면 안됨. 새로운 타입이 추가되었을 경우 undefined 로 될수 있음.
        if (!ServiceType.isWas(serviceTypeCode)) {
            return false;
        }
        // TODO 여기서 undefined가 발생함. 이미 모르는 type이 추가 되었을때 어떻게 해야 하는가?
        // histogramSchema의 type이라도 있으면 유추가 되기는 함. code + schemaType 으로 할 경우 서버에서는 판정가능.
        final ServiceType serviceType = ServiceType.findServiceType(serviceTypeCode);
        if (serviceType == ServiceType.UNDEFINED) {
            return false;
        }
        final ResponseKey responseKey = new ResponseKey(parentApplicationName, serviceType);
        final Histogram histogram = getHistogram(responseKey);
        histogram.addResponseTime(millis);
        return true;
    }

    private Histogram getHistogram(ResponseKey responseKey) {
        final Histogram hit = map.get(responseKey);
        if (hit != null) {
            return hit;
        }
        final Histogram histogram = new LongAdderHistogram(responseKey.getServiceType());
        final Histogram old = map.putIfAbsent(responseKey, histogram);
        if (old != null) {
            return old;
        }
        return histogram;
    }


    private static final class ResponseKey {
        private final ServiceType serviceType;
        private final String parentApplicationName;

        private ResponseKey(String parentApplicationName, ServiceType serviceType) {
            if (parentApplicationName == null) {
                throw new NullPointerException("parentApplicationName must not be null");
            }
            if (serviceType == null) {
                throw new NullPointerException("serviceType must not be null");
            }

            this.parentApplicationName = parentApplicationName;
            this.serviceType = serviceType;
        }

        public String getParentApplicationName() {
            return parentApplicationName;
        }

        public ServiceType getServiceType() {
            return serviceType;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ResponseKey that = (ResponseKey) o;

            if (!parentApplicationName.equals(that.parentApplicationName)) return false;
            if (serviceType != that.serviceType) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = serviceType.hashCode();
            result = 31 * result + parentApplicationName.hashCode();
            return result;
        }
    }
}
