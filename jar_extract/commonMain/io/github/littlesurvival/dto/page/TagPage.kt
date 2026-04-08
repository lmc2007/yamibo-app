package io.github.littlesurvival.dto.page

import io.github.littlesurvival.dto.model.PageNav
import io.github.littlesurvival.dto.model.ThreadSummary

data class TagPage(
    val threadSummaries : List<ThreadSummary>,
    val pageNav: PageNav? = null
)