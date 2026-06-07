import { createSlice } from "@reduxjs/toolkit";

const errorSlice = createSlice({
    name:"error",
    initialState:{
        counter: 0,
        errors: []
    },
    reducers:{
        addError : (state, action)=>{
            // add item with counter and increment it
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

export const {addError, removeError} = authSlice.actions;
export default errorSlice.reducer;