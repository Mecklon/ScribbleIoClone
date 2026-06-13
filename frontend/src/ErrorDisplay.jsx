import React from "react";
import { useSelector } from "react-redux";
import { AnimatePresence } from "motion/react";
import { motion } from "motion/react";
function ErrorDisplay() {
  const { errors } = useSelector((store) => store.error);
  console.log(errors);
  return (
    <div className="fixed bottom-0 flex flex-col items-center justify-center w-full pb-3 text-xl gap-1">
      <AnimatePresence>
        {errors.map((error) => {
          return error.type === "ERROR" ? (
            <motion.div
              key={error.id}
              initial={{ scale: 0, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              layout
              transition={{ duration: 0.3 }}
              className="text-white bg-red-800 border-2 border-red-500 px-4 py-1 rounded-md"
            >{`${error.exceptionType} : ${error.httpStatus ? error.httpStatus : ""} : ${error.message}`}</motion.div>
          ) : (
            <motion.div
              className="bg-blue-950 text-white border-2 border-blue-600 px-4 py-1 rounded-md"
              key={error.id}
            >
              {error.message}
            </motion.div>
          );
        })}
      </AnimatePresence>
    </div>
  );
}

export default ErrorDisplay;
