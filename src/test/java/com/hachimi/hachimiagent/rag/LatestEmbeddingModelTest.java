package com.hachimi.hachimiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
@Slf4j
public class LatestEmbeddingModelTest {

    @Autowired
    @Qualifier("siliconFlowEmbeddingModel")
    private EmbeddingModel siliconFlowEmbeddingModel;

    @Value("${spring.ai.model.embedding:}")
    private String embeddingModelConfig;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.embedding.options.model}")
    private String modelName;

    @Test
    public void testLatestConfigurationProperties() {
        log.info("🧪 验证最新Spring AI配置属性...");

        // 验证新的模型启用配置
        assertEquals("openai", embeddingModelConfig,
                "应该使用新的spring.ai.model.embedding=openai配置");

        // 验证硅基流动配置
        assertEquals("https://api.siliconflow.cn/v1", baseUrl,
                "Base URL应该指向硅基流动");
        assertEquals("netease-youdao/bce-embedding-base_v1", modelName,
                "模型应该是网易有道的嵌入模型");

        log.info("✅ 最新配置属性验证通过:");
        log.info("   🔧 嵌入模型配置: {}", embeddingModelConfig);
        log.info("   🌐 Base URL: {}", baseUrl);
        log.info("   🤖 Model: {}", modelName);
    }

    @Test
    public void testLatestEmbeddingModelType() {
        log.info("🧪 验证最新嵌入模型类型...");

        assertNotNull(siliconFlowEmbeddingModel, "嵌入模型不应为空");
        assertTrue(siliconFlowEmbeddingModel instanceof OpenAiEmbeddingModel,
                "应该是OpenAiEmbeddingModel类型");

        log.info("✅ 嵌入模型类型验证通过: {}",
                siliconFlowEmbeddingModel.getClass().getSimpleName());
    }

    @Test
    public void testLatestEmbeddingAPICall() {
        log.info("🧪 测试最新嵌入API调用方式...");

        try {
            // 使用最新的embedForResponse方法
            List<String> texts = List.of(
                    "测试最新Spring AI嵌入功能",
                    "Hello World with latest API",
                    "人工智能向量嵌入测试"
            );

            EmbeddingResponse response = siliconFlowEmbeddingModel.embedForResponse(texts);

            // 验证响应
            assertNotNull(response, "响应不应为空");
            assertNotNull(response.getResults(), "嵌入结果不应为空");
            assertEquals(texts.size(), response.getResults().size(),
                    "返回的嵌入向量数量应与输入文本数量一致");

            // 检查每个嵌入结果
            for (int i = 0; i < response.getResults().size(); i++) {
                var embedding = response.getResults().get(i);
                assertNotNull(embedding.getOutput(),
                        String.format("第%d个嵌入向量不应为空", i + 1));

                float[] vector = embedding.getOutput();
                assertTrue(vector.length > 0, "嵌入向量长度应大于0");

                log.info("✅ 文本 '{}' -> 向量维度: {}, 索引: {}",
                        texts.get(i), vector.length, embedding.getIndex());
            }

            // 检查响应元数据
            if (response.getMetadata() != null) {
                log.info("📋 响应元数据: {}", response.getMetadata());
            }

            log.info("✅ 最新嵌入API调用测试成功!");

        } catch (Exception e) {
            log.error("❌ 最新嵌入API调用失败", e);
            fail("最新嵌入API调用失败: " + e.getMessage());
        }
    }

    @Test
    public void testRuntimeOptionsOverride() {
        log.info("🧪 测试运行时选项覆盖...");

        try {
            // 创建运行时选项来覆盖默认配置
            OpenAiEmbeddingOptions runtimeOptions = OpenAiEmbeddingOptions.builder()
                    .model("netease-youdao/bce-embedding-base_v1")  // 明确指定模型
                    .encodingFormat("float")                        // 指定编码格式
                    .user("test-user")                             // 指定用户ID
                    .build();

            // 使用运行时选项创建请求
            EmbeddingRequest request = new EmbeddingRequest(
                    List.of("运行时选项测试"),
                    runtimeOptions
            );

            EmbeddingResponse response = siliconFlowEmbeddingModel.call(request);

            assertNotNull(response, "响应不应为空");
            assertFalse(response.getResults().isEmpty(), "应该有嵌入结果");

            float[] vector = response.getResults().get(0).getOutput();
            assertTrue(vector.length > 0, "嵌入向量长度应大于0");

            log.info("✅ 运行时选项覆盖测试成功，向量维度: {}", vector.length);

        } catch (Exception e) {
            log.error("❌ 运行时选项覆盖测试失败", e);
            fail("运行时选项覆盖测试失败: " + e.getMessage());
        }
    }

    @Test
    public void testChineseTextProcessing() {
        log.info("🧪 测试中文文本处理（网易有道模型优势）...");

        try {
            List<String> chineseTexts = List.of(
                    "春江潮水连海平，海上明月共潮生",
                    "人工智能技术的发展改变了世界",
                    "中华文化博大精深，源远流长"
            );

            EmbeddingResponse response = siliconFlowEmbeddingModel.embedForResponse(chineseTexts);

            assertNotNull(response, "中文嵌入响应不应为空");
            assertEquals(chineseTexts.size(), response.getResults().size(),
                    "中文嵌入结果数量应正确");

            for (int i = 0; i < response.getResults().size(); i++) {
                float[] vector = response.getResults().get(i).getOutput();
                assertTrue(vector.length > 0, "中文嵌入向量长度应大于0");

                // 检查向量是否有效（不全为零）
                boolean hasNonZeroValue = false;
                for (float value : vector) {
                    if (Math.abs(value) > 1e-10) {
                        hasNonZeroValue = true;
                        break;
                    }
                }
                assertTrue(hasNonZeroValue, "中文嵌入向量不应该全为零");

                log.info("📝 中文文本 '{}' -> 向量维度: {}",
                        chineseTexts.get(i), vector.length);
            }

            log.info("✅ 中文文本处理测试通过!");

        } catch (Exception e) {
            log.error("❌ 中文文本处理测试失败", e);
            fail("中文文本处理测试失败: " + e.getMessage());
        }
    }

    @Test
    public void testEmbeddingConsistency() {
        log.info("🧪 测试嵌入一致性...");

        try {
            String testText = "一致性测试文本";

            // 多次调用同一文本
            EmbeddingResponse response1 = siliconFlowEmbeddingModel.embedForResponse(List.of(testText));
            EmbeddingResponse response2 = siliconFlowEmbeddingModel.embedForResponse(List.of(testText));

            float[] vector1 = response1.getResults().get(0).getOutput();
            float[] vector2 = response2.getResults().get(0).getOutput();

            assertEquals(vector1.length, vector2.length, "两次调用的向量维度应该一致");

            // 计算余弦相似度
            double similarity = calculateCosineSimilarity(vector1, vector2);
            assertTrue(similarity > 0.99,
                    String.format("同一文本的两次嵌入结果应该高度相似，当前相似度: %.6f", similarity));

            log.info("✅ 嵌入一致性测试通过，相似度: {:.6f}", similarity);

        } catch (Exception e) {
            log.error("❌ 嵌入一致性测试失败", e);
            fail("嵌入一致性测试失败: " + e.getMessage());
        }
    }

    // 辅助方法：计算余弦相似度
    private double calculateCosineSimilarity(float[] vector1, float[] vector2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}