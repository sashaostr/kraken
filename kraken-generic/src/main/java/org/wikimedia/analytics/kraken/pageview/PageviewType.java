/**
 *Copyright (C) 2012-2013  Wikimedia Foundation
 *
 *This program is free software; you can redistribute it and/or
 *modify it under the terms of the GNU General Public License
 *as published by the Free Software Foundation; either version 2
 *of the License, or (at your option) any later version.
 *
 *This program is distributed in the hope that it will be useful,
 *but WITHOUT ANY WARRANTY; without even the implied warranty of
 *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *GNU General Public License for more details.
 *
 *You should have received a copy of the GNU General Public License
 *along with this program; if not, write to the Free Software
 *Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 */
package org.wikimedia.analytics.kraken.pageview;


import java.net.URL;

/**
 * This clas defines the different types of pageviews on the Wikimedia properties.
 */
public enum PageviewType {
    /** A regular mobile pageview, url contains .m. but not api.php */
    MOBILE,

    /** A mobile pageview requested through the api, url contains .m. and also api.php */
    MOBILE_API,

    /** A mobile search request, url contains .m. and the string 'search' */
    MOBILE_SEARCH,

    /** A mobile zero request, url contains .zero. */
    MOBILE_ZERO,

    /** A regular pageview */
    DESKTOP,

    /** A regular API request */
    DESKTOP_API,

    /** A regular search request */
    DESKTOP_SEARCH,

    /** A pageview on the blog */
    BLOG,

    /** An image from commons or upload */
    COMMONS_IMAGE,

    /** A banner served from meta */
    BANNER,

    /** Other type of request */
    OTHER,

    /** Not a valid webrequest */
    NONE;


    /**
     * Given a url, determine the pageview type (mobile, desktop, api, search and blog).
     */
    public static final PageviewType determinePageviewType(final URL url) {
        if (url != null) {
            if (url.getQuery() != null && url.getQuery().contains("bannerloader")) {
                return PageviewType.BANNER;
            } else if (url.getHost().contains("upload")) {
                return PageviewType.COMMONS_IMAGE;
            } else if (url.getHost().contains(".m.")) {
                if (url.getPath().contains("api.php")) {
                    if (url.getQuery() != null && url.getQuery().contains("opensearch")) {
                        return PageviewType.MOBILE_SEARCH;
                    } else {
                        return PageviewType.MOBILE_API;
                    }
                } else if (url.getQuery() != null && url.getQuery().contains("search")) {
                    return PageviewType.MOBILE_SEARCH;
                } else {
                    return PageviewType.MOBILE;
                }
            } else if (url.getHost().contains(".zero.")) {
                return PageviewType.MOBILE_ZERO;
            } else if (url.getHost().contains("wiki")) { // FIXME: obviously wrong
                if (url.getPath().contains("api.php")) {
                    if (url.getQuery() != null && url.getQuery().contains("opensearch")) {
                        return PageviewType.DESKTOP_SEARCH;
                    } else {
                        return PageviewType.DESKTOP_API;
                    }
                } else if (url.getQuery() != null && url.getQuery().contains("search")) {
                    return PageviewType.DESKTOP_SEARCH;
                } else {
                    return PageviewType.DESKTOP;
                }

            } else if (url.getHost().contains("wikimediafoundation") && url.getPath().contains("blog")) {
                return PageviewType.BLOG;
            } else {
                return PageviewType.OTHER;
            }
        } else {
            return PageviewType.NONE;
        }
    }
}

