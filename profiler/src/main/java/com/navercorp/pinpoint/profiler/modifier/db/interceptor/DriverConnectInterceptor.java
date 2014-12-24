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

package com.navercorp.pinpoint.profiler.modifier.db.interceptor;

import com.navercorp.pinpoint.bootstrap.context.DatabaseInfo;
import com.navercorp.pinpoint.bootstrap.context.RecordableTrace;
import com.navercorp.pinpoint.bootstrap.instrument.Scope;
import com.navercorp.pinpoint.bootstrap.interceptor.*;
import com.navercorp.pinpoint.bootstrap.interceptor.tracevalue.DatabaseInfoTraceValueUtils;
import com.navercorp.pinpoint.bootstrap.util.InterceptorUtils;


/**
 * @author emeroad
 */
public class DriverConnectInterceptor extends SpanEventSimpleAroundInterceptor {

    private final Scope scope;
    private final boolean recordConnection;


    public DriverConnectInterceptor(Scope scope) {
        this(true, scope);
    }

    public DriverConnectInterceptor(boolean recordConnection, Scope scope) {
        super(DriverConnectInterceptor.class);
        if (scope == null) {
            throw new NullPointerException("scope must not be null");
        }
        // mysql loadbalance 전용옵션 실제 destination은 하위의 구현체에서 레코딩한다.
        this.recordConnection = recordConnection;
        this.scope = scope;
    }

    @Override
    protected void logBeforeInterceptor(Object target, Object[] args) {
        // parameter에 암호가 포함되어 있음 로깅하면 안됨.
        logger.beforeInterceptor(target, null);
    }

    @Override
    protected void prepareBeforeTrace(Object target, Object[] args) {
        scope.push();
    }

    @Override
    protected void doInBeforeTrace(RecordableTrace trace, Object target, Object[] args) {
        trace.markBeforeTime();
    }


    @Override
    protected void logAfterInterceptor(Object target, Object[] args, Object result, Throwable throwable) {
        logger.afterInterceptor(target, null, result, throwable);
    }

    @Override
    protected void prepareAfterTrace(Object target, Object[] args, Object result, Throwable throwable) {
        // 여기서는 trace context인지 아닌지 확인하면 안된다. trace 대상 thread가 아닌곳에서 connection이 생성될수 있음.
        scope.pop();

        final boolean success = InterceptorUtils.isSuccess(throwable);
        // 여기서는 trace context인지 아닌지 확인하면 안된다. trace 대상 thread가 아닌곳에서 connection이 생성될수 있음.
        final String driverUrl = (String) args[0];
        DatabaseInfo databaseInfo = createDatabaseInfo(driverUrl);
        if (success) {
            if (recordConnection) {
                DatabaseInfoTraceValueUtils.__setTraceDatabaseInfo(result, databaseInfo);
            }
        }
    }

    @Override
    protected void doInAfterTrace(RecordableTrace trace, Object target, Object[] args, Object result, Throwable throwable) {

        if (recordConnection) {
            final DatabaseInfo databaseInfo = DatabaseInfoTraceValueUtils.__getTraceDatabaseInfo(result, UnKnownDatabaseInfo.INSTANCE);
            // database connect도 매우 무거운 액션이므로 카운트로 친다.
            trace.recordServiceType(databaseInfo.getExecuteQueryType());
            trace.recordEndPoint(databaseInfo.getMultipleHost());
            trace.recordDestinationId(databaseInfo.getDatabaseId());
        }
        final String driverUrl = (String) args[0];
        // 여기서 databaseInfo.getRealUrl을 하면 위험하다. loadbalance connection일때 원본 url이 아닌 url이 오게 되어 있음.
        trace.recordApiCachedString(getMethodDescriptor(), driverUrl, 0);

        trace.recordException(throwable);
        trace.markAfterTime();
    }

    private DatabaseInfo createDatabaseInfo(String url) {
        if (url == null) {
            return UnKnownDatabaseInfo.INSTANCE;
        }
        final DatabaseInfo databaseInfo = getTraceContext().parseJdbcUrl(url);
        if (isDebug) {
            logger.debug("parse DatabaseInfo:{}", databaseInfo);
        }
        return databaseInfo;
    }

}
