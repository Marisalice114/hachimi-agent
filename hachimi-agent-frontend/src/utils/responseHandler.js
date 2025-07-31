/**
 * 统一处理后端BaseResponse响应
 * 后端响应格式: { code: number, data: T, message: string }
 */
 
/**
 * 处理API响应，提取data字段
 * @param {Object} response - 后端返回的BaseResponse对象
 * @returns {any} data字段的值
 * @throws {Error} 当code不为0时抛出错误
 */
export const handleResponse = (response) => {
  if (!response) {
    throw new Error('响应数据为空')
  }
  
  // 如果是直接的axios响应对象，提取data
  const responseData = response.data || response
  
  // 如果已经是处理过的数据（不包含BaseResponse结构），直接返回
  if (!responseData.hasOwnProperty('code')) {
    return responseData
  }
  
  if (responseData.code === 0) {
    return responseData.data
  } else {
    const errorMsg = responseData.message || `请求失败: ${responseData.code}`
    console.error('API错误:', errorMsg, responseData)
    throw new Error(errorMsg)
  }
}
 
/**
 * 处理可能包含BaseResponse的响应
 * @param {any} data - 响应数据
 * @returns {any} 处理后的数据
 */
export const processData = (data) => {
  // 如果数据包含BaseResponse结构，提取data字段
  if (data && typeof data === 'object' && 'code' in data) {
    if (data.code === 0) {
      return data.data
    } else {
      throw new Error(data.message || `请求失败: ${data.code}`)
    }
  }
  return data
}
 
export default {
  handleResponse,
  processData
}