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

package com.navercorp.pinpoint.profiler.metadata;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author emeroad
 */
public class AgentIdentifierCompareTest {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void test() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long l = System.currentTimeMillis();
        Date date = new Date(l);
        logger.debug(simpleDateFormat.format(date));

        int max = Integer.MAX_VALUE;
        Date maxAfter = new Date(l+max);
        logger.debug(simpleDateFormat.format(maxAfter));
//          Agent의 identifer대신에 서버시작시간 - 실행시간을 구해서 int type으로 전달하는건 좋은 생각아 아님
//         int max를 더하더라도 최대 한달 정도가 한계임, unsigned type으로 해도 두달 그냥 사이즈 빼서 가변 인코딩 long으로 보내야 함.
//        2013-05-25 15:39:09
//        2013-04-30 19:07:45




    }
}
