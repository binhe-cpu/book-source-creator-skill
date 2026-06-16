package io.legado.validator

import io.legado.validator.analyzeRule.AnalyzeUrl
import io.legado.validator.model.BookSource
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AnalyzeUrlTest {
    @Test
    fun `relative URL resolves against baseUrl`() {
        val source = BookSource(bookSourceUrl = "http://appi.kuwo.cn")
        val au = AnalyzeUrl(
            mUrl = "/novels/api/book/search?keyword={{key}}&pi={{page}}",
            key = "test", page = 1,
            source = source
        )
        assertEquals("http://appi.kuwo.cn/novels/api/book/search?keyword=test&pi=1", au.url)
    }
}
