// "Siste nytt" from NRK's RSS feeds, fetched straight from the browser: NRK sends
// Access-Control-Allow-Origin: * and caches the feed on its CDN, so no backend is involved and
// visitors never wait for a sleeping server. Only headline, date and a one-sentence teaser are
// shown, with a link to NRK - never the article text the feed carries.
import { useEffect, useState } from "react";

/**
 * Teams with NRK coverage. NRK moved the men's national team from the tag "Landslaget i fotball,
 * menn" (which has its own feed, no longer updated) to "Fotballandslaget, menn" (no feed of its
 * own) in October 2026. So the general sport feeds are read too and filtered on either tag.
 */
const FEEDS: Record<string, { rss: string[]; categories: string[]; page: string }> = {
  norway: {
    rss: [
      "https://www.nrk.no/sport/toppsaker.rss",
      "https://www.nrk.no/sport/siste.rss",
      "https://www.nrk.no/sport/landslaget-i-fotball_-menn-1.11613046.rss",
    ],
    categories: ["Fotballandslaget, menn", "Landslaget i fotball, menn"],
    page: "https://www.nrk.no/fotballandslaget/",
  },
};

const MAX_ITEMS = 5;
const TEASER_MAX_CHARS = 160;
// Links from the feed are rendered as hrefs, so only NRK article URLs are accepted.
const ALLOWED_LINK = /^https:\/\/(www\.)?nrk\.no\//;

export interface NewsItem {
  title: string;
  link: string;
  published: Date | null;
  teaser: string | null;
  categories: string[];
}

/** NRK's page for the team, for the "Kilde" link. */
export function newsSourcePage(teamId: string): string | null {
  return FEEDS[teamId]?.page ?? null;
}

/** First sentence of the description, as plain text and capped in length. */
export function teaser(description: string | null): string | null {
  if (!description) return null;
  const text = new DOMParser().parseFromString(description, "text/html").body.textContent?.trim() ?? "";
  const firstSentence = text.split(/(?<=[.!?])\s/)[0] ?? "";
  if (!firstSentence) return null;
  return firstSentence.length <= TEASER_MAX_CHARS
    ? firstSentence
    : `${firstSentence.slice(0, TEASER_MAX_CHARS).replace(/\s+\S*$/, "")}…`;
}

export function parseFeed(xml: string): NewsItem[] {
  const doc = new DOMParser().parseFromString(xml, "application/xml");
  return [...doc.querySelectorAll("item")]
    .map((item) => {
      const text = (tag: string) => item.querySelector(tag)?.textContent?.trim() ?? null;
      const pubDate = text("pubDate");
      const published = pubDate ? new Date(pubDate) : null;
      return {
        title: text("title") ?? "",
        link: text("link") ?? "",
        published: published && !Number.isNaN(published.getTime()) ? published : null,
        teaser: teaser(text("description")),
        categories: [...item.querySelectorAll("category")].map((c) => c.textContent?.trim() ?? ""),
      };
    })
    .filter((item) => item.title && ALLOWED_LINK.test(item.link));
}

/** Items tagged with one of {@code categories}, from several feeds: de-duplicated, newest first. */
export function mergeFeeds(feeds: NewsItem[][], categories: string[]): NewsItem[] {
  const byLink = new Map<string, NewsItem>();
  for (const item of feeds.flat()) {
    if (item.categories.some((c) => categories.includes(c)) && !byLink.has(item.link)) {
      byLink.set(item.link, item);
    }
  }
  return [...byLink.values()]
    .sort((a, b) => (b.published?.getTime() ?? 0) - (a.published?.getTime() ?? 0))
    .slice(0, MAX_ITEMS);
}

export function useTeamNews(teamId: string): { items: NewsItem[]; loading: boolean; failed: boolean } {
  const [state, setState] = useState({ items: [] as NewsItem[], loading: true, failed: false });

  useEffect(() => {
    const feed = FEEDS[teamId];
    if (!feed) return;
    const controller = new AbortController();
    setState({ items: [], loading: true, failed: false });
    // allSettled: one feed failing must not hide the news found in the others.
    Promise.allSettled(
      feed.rss.map((url) =>
        fetch(url, { signal: controller.signal }).then((response) => {
          if (!response.ok) throw new Error(`NRK ${response.status} for ${url}`);
          return response.text();
        }),
      ),
    ).then((results) => {
      if (controller.signal.aborted) return;
      const parsed = results.flatMap((r) => (r.status === "fulfilled" ? [parseFeed(r.value)] : []));
      results
        .filter((r): r is PromiseRejectedResult => r.status === "rejected")
        .forEach((r) => console.warn("Siste nytt: en NRK-feed kunne ikke hentes", r.reason));
      setState({ items: mergeFeeds(parsed, feed.categories), loading: false, failed: parsed.length === 0 });
    });
    return () => controller.abort();
  }, [teamId]);

  return state;
}
