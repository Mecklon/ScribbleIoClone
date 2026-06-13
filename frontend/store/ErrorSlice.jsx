import { createSlice } from "@reduxjs/toolkit";

const errorSlice = createSlice({
    name:"error",
    initialState:{
        errors: []
    },
    reducers:{
        addError : (state, action)=>{
            state.errors.push(action.payload);
        },
        removeError: (state, action)=>{
            state.errors = state.errors.filter(error=> error.id!==action.payload.id)
        }
    }
})

export const addErrorWithTimeout =
  (errorInfo, timeout = 5000) =>
  dispatch => {
    const id = crypto.randomUUID();

    dispatch(
      addError({
        id,
        ...errorInfo
      })
    );

    setTimeout(() => {
      dispatch(removeError({ id }));
    }, timeout);
  };

export const {addError, removeError} = errorSlice.actions;
export default errorSlice.reducer;