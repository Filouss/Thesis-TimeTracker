import { useEffect } from "react";
import { useLocation } from "react-router-dom";
import { useIssues } from "../../context/IssueContext";

function setFavicon(iconPath: string) {
  const faviconLink = document.querySelector<HTMLLinkElement>("#dynamic-favicon");
  if (!faviconLink) return;
  faviconLink.href = iconPath;
}

export default function FaviconTracker() {
  const location = useLocation();
  const { data } = useIssues();

  useEffect(() => {
    if (location.pathname.endsWith("/landing")) {
      setFavicon("/hourglass.svg");
      return;
    }

    if (!data?.tracking) {
      setFavicon("/hourglass.svg");
      return;
    }

    if (data.trackingPaused) {
      setFavicon("/hourglass-stopped.svg");
      return;
    }

    setFavicon("/hourglass-active.svg");
  }, [data?.tracking?.id, data?.trackingPaused, location.pathname]);

  return null;
}
