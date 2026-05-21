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
        const res = await api.get(`http://localhost:9090/api/files/${path}`, {
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
    return <div>Loading.....</div>;
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
