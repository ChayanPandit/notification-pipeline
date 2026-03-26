package com.notifpipeline

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class NotificationPipelineApplication

fun main(args: Array<String>) {
	runApplication<NotificationPipelineApplication>(*args)
}
