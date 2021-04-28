package io.github.gmathi.novellibrary.model.source

import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import rx.Observable

/**
 * A basic interface for creating a source. It could be an online source, a local source, etc...
 */
interface Source {

    /**
     * Id for the source. Must be unique.
     */
    val id: Long

    /**
     * Name of the source.
     */
    val name: String

    /**
     * Returns an observable with the updated details for a novel.
     *
     * @param novel the novel to update.
     */
    fun fetchNovelDetails(novel: Novel): Observable<Novel>

    /**
     * Returns an observable with all the available chapters for a novel.
     *
     * @param novel the novel to update.
     */
    fun fetchChapterList(novel: Novel): Observable<List<WebPage>>



}