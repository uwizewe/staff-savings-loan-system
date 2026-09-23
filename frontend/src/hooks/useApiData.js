import { useCallback, useEffect, useState } from "react";
import { get } from "../services/api.js";

export function useApiData(path, dependencies = []) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const reload = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setData(await get(path));
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, [path, ...dependencies]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { reload(); }, [reload]);
  return { data, loading, error, reload, setData };
}

