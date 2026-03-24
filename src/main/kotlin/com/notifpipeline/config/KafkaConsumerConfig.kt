package com.notifpipeline.config

import com.notifpipeline.messaging.model.NotificationEvent
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.JsonDeserializer

@Configuration
class KafkaConsumerConfig {

    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Bean
    fun consumerFactory(): ConsumerFactory<String, NotificationEvent> {
        val deserializer = JsonDeserializer(NotificationEvent::class.java).apply {
            addTrustedPackages("*")
        }
        val props = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false  // manual ack only
        )
        return DefaultKafkaConsumerFactory(props, StringDeserializer(), deserializer)
    }

    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, NotificationEvent> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, NotificationEvent>()
        factory.consumerFactory = consumerFactory()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        factory.setConcurrency(3)  // 3 threads, each handling a subset of partitions
        return factory
    }
}