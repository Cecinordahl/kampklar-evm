// "Siste nytt" from NRK's RSS feeds, fetched straight from the browser: NRK sends
// Access-Control-Allow-Origin: * and caches the feed on its CDN, so no backend is involved and
// visitors never wait for a sleeping server. Only headline, date and a one-sentence teaser are
// shown, with a link to NRK - never the article text the feed carries.
import { useEffect, useState } from "react";

/** Teams with a dedicated NRK feed. */
const FEEDS: Record<string, { rss: string; page: string }> = {
  norway: {
    rss: "https://www.nrk.no/sport/landslaget-i-fotball_-menn-1.11613046.rss",
    page: "https://www.nrk.no/sport/landslaget-i-fotball_-menn-1.11613046",
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
      };
    })
    .filter((item) => item.title && ALLOWED_LINK.test(item.link))
    .slice(0, MAX_ITEMS);
}

export function useTeamNews(teamId: string): { items: NewsItem[]; loading: boolean; failed: boolean } {
  const [state, setState] = useState({ items: [] as NewsItem[], loading: true, failed: false });

  useEffect(() => {
    const url = FEEDS[teamId]?.rss;
    if (!url) return;
    const controller = new AbortController();
    setState({ items: [], loading: true, failed: false });
    fetch(url, { signal: controller.signal })
      .then((response) => {
        if (!response.ok) throw new Error(`NRK ${response.status}`);
        return response.text();
      })
      .then((xml) => setState({ items: parseFeed(xml), loading: false, failed: false }))
      .catch((err: unknown) => {
        if (!controller.signal.aborted) setState({ items: [], loading: false, failed: true });
        if (!(err instanceof DOMException)) console.warn("Siste nytt kunne ikke hentes", err);
      });
    return () => controller.abort();
  }, [teamId]);

  return state;
}
