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

import java.util.HashMap;
import java.util.Map;

import com.navercorp.pinpoint.bootstrap.context.DatabaseInfo;
import com.navercorp.pinpoint.bootstrap.context.Trace;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.interceptor.ByteCodeMethodDescriptorSupport;
import com.navercorp.pinpoint.bootstrap.interceptor.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.interceptor.SimpleAroundInterceptor;
import com.navercorp.pinpoint.bootstrap.interceptor.TraceContextSupport;
import com.navercorp.pinpoint.bootstrap.interceptor.tracevalue.BindValueTraceValue;
import com.navercorp.pinpoint.bootstrap.interceptor.tracevalue.DatabaseInfoTraceValueUtils;
import com.navercorp.pinpoint.bootstrap.interceptor.tracevalue.ParsingResultTraceValue;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.common.util.ParsingResult;

/**
 * @author emeroad
 */
public class PreparedStatementExecuteQueryInterceptor implements SimpleAroundInterceptor, ByteCodeMethodDescriptorSupport, TraceContextSupport {

    private static final int DEFAULT_BIND_VALUE_LENGTH = 1024;

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private MethodDescriptor descriptor;
    private TraceContext traceContext;
    private int maxSqlBindValueLength = DEFAULT_BIND_VALUE_LENGTH;

    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }

        Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }

        trace.traceBlockBegin();
        trace.markBeforeTime();
        try {
            DatabaseInfo databaseInfo = DatabaseInfoTraceValueUtils.__getTraceDatabaseInfo(target, UnKnownDatabaseInfo.INSTANCE);
            trace.recordServiceType(databaseInfo.getExecuteQueryType());

            trace.recordEndPoint(databaseInfo.getMultipleHost());
            trace.recordDestinationId(databaseInfo.getDatabaseId());

            ParsingResult parsingResult = null;
            if (target instanceof ParsingResultTraceValue) {
                parsingResult = ((ParsingResultTraceValue) target).__getTraceParsingResult();
            }
            Map<Integer, String> bindValue = null;
            if (target instanceof BindValueTraceValue) {
                bindValue = ((BindValueTraceValue)target).__getTraceBindValue();
            }
            if (bindValue != null) {
                String bindString = toBindVariable(bindValue);
                trace.recordSqlParsingResult(parsingResult, bindString);
            } else {
                trace.recordSqlParsingResult(parsingResult);
            }

            trace.recordApi(descriptor);
//            trace.recordApi(apiId);
            // clean 타이밍을 변경해야 될듯 하다.
            // clearParameters api가 따로 있으나, 구지 캡쳐 하지 않아도 될듯함.시간남으면 하면 좋기는 함.
            // ibatis 등에서 확인해봐도 cleanParameters 의 경우 대부분의 경우 일부러 호출하지 않음.
            clean(target);


        } catch (Exception e) {
            if (logger.isWarnEnabled()) {
                logger.warn(e.getMessage(), e);
            }
        }

    }

    private void clean(Object target) {
        if (target instanceof BindValueTraceValue) {
            ((BindValueTraceValue) target).__setTraceBindValue(new HashMap<Integer, String>());
        }
    }

    private String toBindVariable(Map<Integer, String> bindValue) {
        final String[] temp = new String[bindValue.size()];
        for (Map.Entry<Integer, String> entry : bindValue.entrySet()) {
            Integer key = entry.getKey() - 1;
            if (temp.length < key) {
                continue;
            }
            temp[key] = entry.getValue();
        }

        return BindValueUtils.bindValueToString(temp, maxSqlBindValueLength);

    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args, result, throwable);
        }

        Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }

        try {
            // TODO 일단 테스트로 실패일경우 종료 아닐경우 resultset fetch까지 계산. fetch count는 옵션으로 빼는게 좋을듯.
            trace.recordException(throwable);
            trace.markAfterTime();
        } finally {
            trace.traceBlockEnd();
        }
    }

    @Override
    public void setMethodDescriptor(MethodDescriptor descriptor) {
        this.descriptor = descriptor;
        traceContext.cacheApi(descriptor);
    }


    @Override
    public void setTraceContext(TraceContext traceContext) {
        this.traceContext = traceContext;
        this.maxSqlBindValueLength = traceContext.getProfilerConfig().getJdbcMaxSqlBindValueSize();
    }
}
