package com.github.cgund98.template.infrastructure.events.publisher

import aws.sdk.kotlin.services.sns.SnsClient
import aws.sdk.kotlin.services.sns.model.PublishRequest
import com.github.cgund98.template.infrastructure.events.registry.BaseEvent
import com.github.cgund98.template.infrastructure.events.serializer.EventSerializer

class SnsEventPublisher(
    private val serializer: EventSerializer,
    private val snsClient: SnsClient,
    private val topicArn: String,
) : EventPublisher {
    override suspend fun <P, T : BaseEvent<P>> publish(event: T) {
        val messageBody = serializer.serialize(event)

        val request =
            PublishRequest {
                message = messageBody
                topicArn = this@SnsEventPublisher.topicArn

                messageAttributes =
                    mapOf(
                        "event_type" to
                            aws.sdk.kotlin.services.sns.model.MessageAttributeValue {
                                dataType = "String"
                                stringValue = event.type.toString()
                            },
                    )
            }

        snsClient.publish(request)
    }
}
