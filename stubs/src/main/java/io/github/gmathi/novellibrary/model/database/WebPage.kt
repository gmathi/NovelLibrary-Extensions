package io.github.gmathi.novellibrary.model.database

data class WebPage(var url: String, var chapterName: String) {


    /**
     * Order of the chapter in the chapters list. The higher the order, the latest released chapter it is.
     */
    var orderId: Long = -1L

    /**
     * In-case the chapter is aggregated from multiple translator sources, this source id determines the source. Default is -1L
     */
    var translatorSourceName: String? = null


    // Other Methods
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WebPage

        if (url != other.url) return false
        if (chapterName != other.chapterName) return false
        if (translatorSourceName != other.translatorSourceName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + chapterName.hashCode()
        return result
    }

    override fun toString(): String {
        return "Chapter(url='$url', chapterName='$chapterName', orderId=$orderId, translatorSourceName=$translatorSourceName)"
    }


}
