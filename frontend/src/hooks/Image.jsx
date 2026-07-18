import { useEffect, useState } from "react";
import api from "../api/api";

const Image = ({
  path = null,
  className = "",
  fallback = null,
  fullToggle = false,
  onLoadCallBack,
}) => {
  const [src, setSrc] = useState(null);
  const [loading, setLoading] = useState(false);
  useEffect(() => {
    if (path === null) return;

    let objectUrl;

    const getData = async () => {
      try {
        setLoading(true);
        const res = await api.get(`http://${window.location.host}/api/files/${path}`, {
          responseType: "blob",
        });
        objectUrl = URL.createObjectURL(res.data);
        setSrc(objectUrl);
      } catch (err) {
        console.log("image fetch err: ", err);
      } finally {
        setLoading(false);
      }
    };

    getData();
    return () => {
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }
    };
  }, [path]);

  if (loading) {
    return (
      <img
        src={fallback}
        className={className}
        alt=""
      />
    );
  }
  return (
    <img
      onLoad={() => {
        if (onLoadCallBack) {
          onLoadCallBack();
        }
      }}
      src={src || fallback}
      className={className}
      alt=""
    />
  );
};
export default Image;
