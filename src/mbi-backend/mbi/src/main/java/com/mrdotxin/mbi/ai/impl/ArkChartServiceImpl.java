package com.mrdotxin.mbi.ai.impl;

import com.mrdotxin.mbi.ai.AIChartService;
import com.volcengine.ark.runtime.model.bot.completion.chat.BotChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

public class ArkChartServiceImpl implements AIChartService {

    @Value("${AI.BotId}")
    private String botId;

    @Resource
    private ArkService arkService;

    @Override
    public String doAIChart(List<String> queries) {

        final List<ChatMessage> messages = new ArrayList<>();
        for (String query : queries) {
            ChatMessage userMessage =
                    ChatMessage.builder()
                            .role(ChatMessageRole.USER)
                            .content(query)
                            .build();
            messages.add(userMessage);
        }

        BotChatCompletionRequest chatCompletionRequest = BotChatCompletionRequest.builder()
                .botId(botId)
                .messages(messages)
                .build();

        StringBuilder builder = new StringBuilder();

        arkService.createBotChatCompletion(chatCompletionRequest)
                .getChoices()
                .forEach(
                        choice -> builder
                                .append(choice.getMessage().getContent())
                                .append('\n')
                );

        return builder.toString();
    }
}
