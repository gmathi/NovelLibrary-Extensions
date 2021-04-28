@file:Suppress("UNCHECKED_CAST")

package io.github.gmathi.novellibrary.model.database

data class Novel(var url: String, var sourceId: Long) {

    public constructor(name: String?, url: String, sourceId: Long) : this(url, sourceId) {
        name?.let { this.name = name }
    }

    /**
     * Name of the novel
     */
    var name: String = "Unknown - Not Found!"

    /**
     * External Id from the source
     */
    var externalNovelId: String? = null

    /**
     * Novel's cover art image url
     */
    var imageUrl: String? = null

    /**
     * Rating of the novel in the decimal format and ranging between (min) 0.0 - 5.0 (max)
     */
    var rating: String? = null

    /**
     * Short description of the novel that is shown at a glance
     */
    var shortDescription: String? = null

    /**
     * Complete description of the novel
     */
    var longDescription: String? = null

    /**
     * List of genres this novel belongs to
     */
    var genres: List<String>? = null

    /**
     * Author(s) of this novel
     */
    var authors: List<String>? = null

    /**
     * Illustrator(s) of this novel
     */
    var illustrator: List<String>? = null

    /**
     * Number of released chapters in this novel
     */
    var chaptersCount: Long = 0L

    /**
     * More metadata of the novel
     */
    var metadata: HashMap<String, String?> = HashMap()


    override fun toString(): String {
        return "Novel(url='$url', sourceId=$sourceId, name='$name', externalNovelId=$externalNovelId, imageUrl=$imageUrl, rating=$rating, shortDescription=$shortDescription, longDescription=$longDescription, genres=$genres, authors=$authors, illustrator=$illustrator, chaptersCount=$chaptersCount, metadata=$metadata)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Novel

        if (url != other.url) return false
        if (sourceId != other.sourceId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + sourceId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (externalNovelId?.hashCode() ?: 0)
        result = 31 * result + (imageUrl?.hashCode() ?: 0)
        result = 31 * result + (rating?.hashCode() ?: 0)
        result = 31 * result + (shortDescription?.hashCode() ?: 0)
        result = 31 * result + (longDescription?.hashCode() ?: 0)
        result = 31 * result + (genres?.hashCode() ?: 0)
        result = 31 * result + (authors?.hashCode() ?: 0)
        result = 31 * result + (illustrator?.hashCode() ?: 0)
        result = 31 * result + chaptersCount.hashCode()
        result = 31 * result + metadata.hashCode()
        return result
    }


}
