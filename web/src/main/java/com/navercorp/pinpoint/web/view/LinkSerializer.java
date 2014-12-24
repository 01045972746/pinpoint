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

package com.navercorp.pinpoint.web.view;

import com.navercorp.pinpoint.web.applicationmap.Link;
import com.navercorp.pinpoint.web.applicationmap.Node;
import com.navercorp.pinpoint.web.applicationmap.histogram.Histogram;
import com.navercorp.pinpoint.web.applicationmap.rawdata.AgentHistogram;
import com.navercorp.pinpoint.web.applicationmap.rawdata.AgentHistogramList;
import com.navercorp.pinpoint.web.vo.Application;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * @author emeroad
 * @author netspider
 */
public class LinkSerializer extends JsonSerializer<Link> {
    @Override
    public void serialize(Link link, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        jgen.writeStartObject();

        jgen.writeStringField("key", link.getLinkName());  // for servermap

        jgen.writeStringField("from", link.getFrom().getNodeName());  // necessary for go.js
        jgen.writeStringField("to", link.getTo().getNodeName()); // necessary for go.js


        writeSimpleNode("sourceInfo", link.getFrom(), jgen);
        writeSimpleNode("targetInfo", link.getTo(), jgen);

        Application filterApplication = link.getFilterApplication();
        jgen.writeStringField("filterApplicationName", filterApplication.getName());
        jgen.writeNumberField("filterApplicationServiceTypeCode", filterApplication.getServiceTypeCode());
        if (link.isWasToWasLink()) {
            writeWasToWasTargetRpcList(link, jgen);
        }

        Histogram histogram = link.getHistogram();
        jgen.writeNumberField("totalCount", histogram.getTotalCount()); // for go.js
        jgen.writeNumberField("errorCount", histogram.getErrorCount());
        jgen.writeNumberField("slowCount", histogram.getSlowCount());

        jgen.writeObjectField("histogram", histogram);

        // 링크별 각 agent가 어떻게 호출했는지 데이터
        writeAgentHistogram("sourceHistogram", link.getSourceList(), jgen);
        writeAgentHistogram("targetHistogram", link.getTargetList(), jgen);

        writeTimeSeriesHistogram(link, jgen);
        writeSourceAgentTimeSeriesHistogram(link, jgen);


//        String state = link.getLinkState();
//        jgen.writeStringField("state", state); // for go.js
        jgen.writeBooleanField("hasAlert", link.getLinkAlert()); // for go.js

        jgen.writeEndObject();
    }

    private void writeWasToWasTargetRpcList(Link link, JsonGenerator jgen) throws IOException {
        // was -> was 연결일 경우 호출실패시의 이벤트를 filtering 하기 위한 추가적인 호출정보를 알려준다.
        jgen.writeFieldName("filterTargetRpcList");
        jgen.writeStartArray();
        Collection<Application> sourceLinkTargetAgentList = link.getSourceLinkTargetAgentList();
        for (Application application : sourceLinkTargetAgentList) {
            jgen.writeStartObject();
            jgen.writeStringField("rpc", application.getName());
            jgen.writeNumberField("rpcServiceTypeCode", application.getServiceTypeCode());
            jgen.writeEndObject();
        }
        jgen.writeEndArray();

    }



    private void writeTimeSeriesHistogram(Link link, JsonGenerator jgen) throws IOException {
        List<ResponseTimeViewModel> sourceApplicationTimeSeriesHistogram = link.getLinkApplicationTimeSeriesHistogram();

        jgen.writeFieldName("timeSeriesHistogram");
        jgen.writeObject(sourceApplicationTimeSeriesHistogram);
    }


    private void writeAgentHistogram(String fieldName, AgentHistogramList agentHistogramList, JsonGenerator jgen) throws IOException {
        jgen.writeFieldName(fieldName);
        jgen.writeStartObject();
        for (AgentHistogram agentHistogram : agentHistogramList.getAgentHistogramList()) {
            jgen.writeFieldName(agentHistogram.getId());
            jgen.writeObject(agentHistogram.getHistogram());
        }
        jgen.writeEndObject();
    }

    private void writeSourceAgentTimeSeriesHistogram(Link link, JsonGenerator jgen) throws IOException {
        AgentResponseTimeViewModelList sourceAgentTimeSeriesHistogram = link.getSourceAgentTimeSeriesHistogram();
        sourceAgentTimeSeriesHistogram.setFieldName("sourceTimeSeriesHistogram");
        jgen.writeObject(sourceAgentTimeSeriesHistogram);
    }

    private void writeSimpleNode(String fieldName, Node node, JsonGenerator jgen) throws IOException {
        jgen.writeFieldName(fieldName);

        jgen.writeStartObject();
        Application application = node.getApplication();
        jgen.writeStringField("applicationName", application.getName());
        jgen.writeStringField("serviceType", application.getServiceType().toString());
        jgen.writeNumberField("serviceTypeCode", application.getServiceTypeCode());
        jgen.writeBooleanField("isWas", application.getServiceType().isWas());
        jgen.writeEndObject();
    }
}
