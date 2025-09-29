# 🚨 AXIA API TROUBLESHOOTING GUIDE

## **OpenAI Common Problems**

### **Authentication Issues**
- ❌ **Error**: `401 Unauthorized`
- 🔧 **Fix**: Check API key format (must start with `sk-proj-` or `sk-`)
- 💡 **Tip**: Regenerate key if old format

### **Rate Limiting**
- ❌ **Error**: `429 Too Many Requests`
- 🔧 **Fix**: Implement exponential backoff
- 💡 **Tip**: Free tier = 3 RPM, Paid = 3500 RPM

### **Quota Issues**
- ❌ **Error**: `insufficient_quota`
- 🔧 **Fix**: Add billing info or upgrade plan
- 💡 **Tip**: GPT-4 costs ~$0.03/1k tokens

## **Anthropic Common Problems**

### **Authentication Issues**  
- ❌ **Error**: `401 authentication_error`
- 🔧 **Fix**: Key must start with `sk-ant-api03-`
- 💡 **Tip**: Anthropic keys are longer than OpenAI

### **Model Access**
- ❌ **Error**: `model_not_found`
- 🔧 **Fix**: Claude-3 requires paid account
- 💡 **Tip**: Start with claude-3-haiku (cheapest)

### **Message Format**
- ❌ **Error**: `invalid_request_error`
- 🔧 **Fix**: Use messages array format, not prompt
- 💡 **Tip**: Different API structure than OpenAI

## **Network Issues**

### **Timeouts**
- ❌ **Error**: `SocketTimeoutException`
- 🔧 **Fix**: Increase OkHttp timeouts to 180s
- 💡 **Tip**: AI inference can be slow

### **SSL Issues**
- ❌ **Error**: `SSLHandshakeException`
- 🔧 **Fix**: Update Android security provider
- 💡 **Tip**: Common on older devices

## **Quick Fixes**

1. **Test API Keys Online First**
   ```bash
   curl -H "Authorization: Bearer YOUR_KEY" \
        https://api.openai.com/v1/models
   ```

2. **Check Account Status**
   - OpenAI: platform.openai.com/usage
   - Anthropic: console.anthropic.com/dashboard

3. **Monitor Costs**
   - Set billing alerts
   - Use cheaper models for testing
   - Implement token counting

## **Fallback Strategy**

1. **Primary**: OpenAI GPT-3.5-turbo
2. **Secondary**: Anthropic Claude-3-haiku  
3. **Fallback**: Local Llama model
4. **Emergency**: Error message to user